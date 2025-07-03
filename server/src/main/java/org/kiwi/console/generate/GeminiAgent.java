package org.kiwi.console.generate;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import lombok.extern.slf4j.Slf4j;

public class GeminiAgent implements Agent {

    public static final String model = "gemini-2.5-pro";

    private final Client client;

    public GeminiAgent(String apiKey) {
        if (apiKey != null) {
            client = Client.builder()
                    .apiKey(apiKey)
                    .build();
        }
        else
            client = null;
    }

    @Override
    public Chat createChat() {
        return new GeminiChat(client.chats.create(model, GenerateContentConfig
                .builder()
                .thinkingConfig(
                        ThinkingConfig.builder()
                                .includeThoughts(true)
                                .build()
                )
                .build()));
    }

    @Slf4j
    private static class GeminiChat implements Chat {

        private final com.google.genai.Chat chat;

        public GeminiChat(com.google.genai.Chat chat) {
            this.chat = chat;
        }

        @Override
        public void send(String text, ChatStreamListener listener, ChatController ctrl) {
            try (var stream = chat.sendMessageStream(text)) {
                for (var resp : stream) {
                    if (ctrl.isAborted())
                        throw new AgentException("Generation aborted by user");
                    var content = resp.candidates().orElseThrow().getFirst().content().orElseThrow();
                    if (content.parts().isEmpty())
                        continue;
                    var parts = content.parts().get();
                    for (Part part : parts) {
                        if (part.text().isPresent()) {
                            if (part.thought().orElse(false))
                                listener.onThought(part.text().get());
                            else
                                listener.onContent(part.text().get());
                        }
                    }
                }
            } catch (AgentException e) {
                throw e;
            } catch (Exception e) {
                throw new AgentException("Gemini internal error", e);
            }
        }

    }


}