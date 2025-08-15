package org.kiwi.console.generate.qwen;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.kiwi.console.util.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class QwenClient {

    private static final String MODEL = "qwen3-coder-plus";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String URL = " https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apikey;

    public QwenClient(String apikey) {
        this.apikey = apikey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build();
        objectMapper = Utils.getObjectMapper();
    }

    @SneakyThrows
    public Stream<Response> send(List<Message> messages) {
        var request = new org.kiwi.console.generate.qwen.Request(MODEL, messages, true, true);
        var json = objectMapper.writeValueAsString(request);
        var httpReq = new Request.Builder()
                .url(URL)
                .header("Authorization", "Bearer " + apikey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .post(RequestBody.create(json, JSON))
                .build();

        BlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        // Atomic reference to hold any terminal exception.
        AtomicReference<Throwable> streamError = new AtomicReference<>();

        var listener = createStreamListener(queue, streamError);

        var eventSource = EventSources.createFactory(httpClient).newEventSource(httpReq, listener);

        var spliterator = new Spliterators.AbstractSpliterator<Response>(Long.MAX_VALUE, Spliterator.ORDERED) {
            @Override
            public boolean tryAdvance(Consumer<? super Response> action) {
                // First, check if a terminal error occurred.
                if (streamError.get() != null) {
                    throw new RuntimeException("Stream failed with an exception", streamError.get());
                }

                try {
                    // Block until an event is available from the listener thread.
                    var event = queue.take();

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

        return StreamSupport.stream(spliterator, false).onClose(eventSource::cancel);
    }

    private static final Response POISON_PILL = new Response(null, List.of());

    private EventSourceListener createStreamListener(BlockingQueue<Response> queue, AtomicReference<Throwable> streamError) {
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
                    if ("[DONE]".equals(data)) {
                        queue.offer(POISON_PILL);
                        return;
                    }

                    var response = objectMapper.readValue(data, Response.class);
                    queue.offer(response);
                } catch (Exception e) {
                    streamError.set(e);
                    queue.offer(POISON_PILL); // Unblock consumer
                }
            }

            @Override
            public void onClosed(@NotNull EventSource eventSource) {
                queue.offer(POISON_PILL); // Signal clean completion
                queue.offer(POISON_PILL);           // Terminate the stream
            }

            @Override
            public void onFailure(@NotNull EventSource eventSource, Throwable t, okhttp3.Response response) {
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


    @SneakyThrows
    public static void main(String[] args) {
        var apikey = Files.readString(Path.of("/Users/leen/develop/qwen_apikey")).trim();
        var client = new QwenClient(apikey);
        try (var s = client.send(List.of(new Message("user", List.of(new TextContent("Who are you")))))) {
            s.forEach(resp -> System.out.print(resp.choices().getFirst().delta().content()));
        }

        System.out.println();
        System.out.println("Done");
    }

}
