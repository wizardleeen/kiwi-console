package org.kiwi.console.generate;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.file.File;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;

import java.util.ArrayList;
import java.util.List;

public class GeminiModel implements Model {

    public final String model;

    private final Client client;

    public GeminiModel(String model, String apiKey) {
        this.model = model;
        if (apiKey != null) {
            client = Client.builder()
                    .apiKey(apiKey)
                    .build();
        }
        else
            client = null;
    }

    @Override
    public Chat createChat(boolean outputThought) {
        return new GeminiChat(client.chats.create(model, GenerateContentConfig
                .builder()
                .thinkingConfig(
                        ThinkingConfig.builder()
                                .includeThoughts(outputThought)
                                .build()
                )
                .build()));
    }

    @Override
    public String getName() {
        return model;
    }

    @Slf4j
    private static class GeminiChat implements Chat {

        private final com.google.genai.Chat chat;

        public GeminiChat(com.google.genai.Chat chat) {
            this.chat = chat;
        }

        @Override
        public void send(String text, List<File> attachments, ChatStreamListener listener, ChatController ctrl) {
            var contents = new ArrayList<Content>();
            contents.add(Content.fromParts(Part.fromText(text)));
            for (File file : attachments) {
                contents.add(Content.fromParts(Part.fromBytes(file.bytes(), file.mimeType())));
            }
            try (var stream = chat.sendMessageStream(contents)) {
                for (var resp : stream) {
                    if (ctrl.isAborted())
                        throw new BusinessException(ErrorCode.TASK_CANCELLED);
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
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new AgentException("Gemini internal error", e);
            }
        }

    }


}