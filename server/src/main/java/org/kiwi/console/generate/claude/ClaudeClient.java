package org.kiwi.console.generate.claude;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.kiwi.console.util.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ClaudeClient {
    private static final String BASE_URL = "https://api.anthropic.com/v1";
    private static final String DEFAULT_MODEL = "claude-opus-4-6";
    private static final String API_VERSION = "2023-06-01";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    // A special marker object to signal the end of the stream.
    private static final StreamEvent POISON_PILL = new StreamEvent.Done();

    public ClaudeClient(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public ClaudeClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = Utils.getObjectMapper();

        var clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES) // Long timeout for streaming
                .writeTimeout(5, TimeUnit.MINUTES);

        this.httpClient = clientBuilder.build();
    }


    Stream<StreamEvent> send(List<Message> messages) {
        var requestPayload = new ClaudeRequest(model,
                32000,
                new Thinking("enabled", 31999),
                messages,
                true);

        Request request;
        try {
            String json = objectMapper.writeValueAsString(requestPayload);
            RequestBody body = RequestBody.create(json, JSON);
            request = new Request.Builder()
                    .url(BASE_URL + "/messages")
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", API_VERSION)
                    .header("Accept", "text/event-stream")
                    .header("Cache-Control", "no-cache")
                    .header("User-Agent", "Claude-Java-Client-OkHttp/1.0")
                    .post(body)
                    .build();
        } catch (JsonProcessingException e) {
            // Fail fast if we can't even create the request.
            throw new RuntimeException("Failed to serialize request payload", e);
        }

        // A thread-safe queue to bridge the async listener and the sync Stream.
        BlockingQueue<StreamEvent> queue = new LinkedBlockingQueue<>();
        // Atomic reference to hold any terminal exception.
        AtomicReference<Throwable> streamError = new AtomicReference<>();

        EventSourceListener listener = createStreamListener(queue, streamError);

        // The EventSource will start processing events on a background thread.
        EventSource eventSource = EventSources.createFactory(httpClient).newEventSource(request, listener);

        // This custom Spliterator will block on queue.take() until an event is available.
        var spliterator = new Spliterators.AbstractSpliterator<StreamEvent>(Long.MAX_VALUE, Spliterator.ORDERED) {
            @Override
            public boolean tryAdvance(Consumer<? super StreamEvent> action) {
                // First, check if a terminal error occurred.
                if (streamError.get() != null) {
                    throw new RuntimeException("Stream failed with an exception", streamError.get());
                }

                try {
                    // Block until an event is available from the listener thread.
                    StreamEvent event = queue.take();

                    // Check for the poison pill to terminate the stream.
                    if (event == POISON_PILL) {
                        // Re-check for error in case it happened while we were waiting
                        if (streamError.get() != null) {
                            throw new RuntimeException("Stream failed with an exception", streamError.get());
                        }
                        return false; // End of stream
                    }

                    action.accept(event);
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    streamError.set(e);
                    throw new RuntimeException("Stream consumption was interrupted", e);
                }
            }
        };

        // Create a stream from the spliterator and ensure the eventSource is cancelled when the stream is closed.
        return StreamSupport.stream(spliterator, false).onClose(eventSource::cancel);
    }

    private EventSourceListener createStreamListener(BlockingQueue<StreamEvent> queue, AtomicReference<Throwable> streamError) {
        return new EventSourceListener() {
            private final StringBuilder fullResponse = new StringBuilder();

            @Override
            public void onEvent(@NotNull EventSource eventSource, String id, String type, @NotNull String data) {
                try {
                    if ("error".equals(type)) {
                        // Handle explicit error events from the API
                        var error = new RuntimeException("Received error event from API: " + data);
                        streamError.set(error);
                        queue.offer(POISON_PILL); // Unblock consumer
                        return;
                    }

                    var streamResponse = objectMapper.readValue(data, StreamResponse.class);
                    StreamEvent event = switch (streamResponse.type()) {
                        case "content_block_delta" -> {
                            var text = streamResponse.delta() != null ? streamResponse.delta().text() : null;
                            if (text != null) {
                                fullResponse.append(text);
                                yield new StreamEvent.ContentDelta(text);
                            }
                            yield null; // No event to emit if delta is empty
                        }
                        case "message_start" -> new StreamEvent.MessageStart();
                        case "content_block_start" -> new StreamEvent.ContentStart();
                        case "content_block_stop" -> new StreamEvent.ContentStop();
                        case "message_stop" -> new StreamEvent.MessageStop();
                        default -> null; // Ignore unknown event types
                    };

                    if (event != null) {
                        queue.offer(event);
                    }
                } catch (Exception e) {
                    streamError.set(e);
                    queue.offer(POISON_PILL); // Unblock consumer
                }
            }

            @Override
            public void onClosed(@NotNull EventSource eventSource) {
                queue.offer(new StreamEvent.Done()); // Signal clean completion
                queue.offer(POISON_PILL);           // Terminate the stream
            }

            @Override
            public void onFailure(@NotNull EventSource eventSource, Throwable t, Response response) {
                Throwable finalError;
                if (response != null) {
                    try {
                        finalError = new RuntimeException("Stream failed with HTTP " + response.code() + ": " + response.body().string(), t);
                    } catch (IOException e) {
                        finalError = new RuntimeException("Stream failed with HTTP " + response.code() + ", could not read body", t);
                    }
                } else {
                    finalError = t;
                }
                streamError.set(finalError);
                queue.offer(POISON_PILL); // Unblock consumer and signal termination
            }
        };
    }


}