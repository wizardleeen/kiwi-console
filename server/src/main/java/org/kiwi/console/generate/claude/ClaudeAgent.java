package org.kiwi.console.generate.claude;

import lombok.SneakyThrows;
import org.kiwi.console.file.File;
import org.kiwi.console.generate.*;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ClaudeAgent implements Agent {

    private final ClaudeClient client;

    public ClaudeAgent(String apikey) {
        this.client = new ClaudeClient(apikey);
    }

    @Override
    public Chat createChat() {
        return new ClaudeChat();
    }

    class ClaudeChat implements Chat {

        private final List<Message> history = new ArrayList<>();

        @Override
        public void send(String text, List<File> attachments, ChatStreamListener listener, ChatController ctrl) {
            var contents = new ArrayList<Content>();
            contents.add(new TextContent(text));
            for (File attachment : attachments) {
                var mimeType = attachment.mimeType();
                if (mimeType.startsWith("image/")) {
                    contents.add(new ImageContent(new ImageSource(
                            "base64",
                            attachment.mimeType(),
                            Base64.getEncoder().encodeToString(attachment.bytes())
                    )));
                } else if (mimeType.startsWith("text/") || mimeType.equals("application/json"))
                    contents.add(new TextContent(new String(attachment.bytes(), StandardCharsets.UTF_8)));
                else
                    throw new BusinessException(ErrorCode.UNSUPPORTED_ATTACHMENT, attachment.mimeType());
            }
            var message = new Message("user", contents);
            history.add(message);
            try (var events = client.streamMessage(history)) {
                var buf = new StringBuilder();
                var it = events.iterator();
                loop: while (it.hasNext()) {
                    if (ctrl.isAborted())
                        throw new AgentException("Aborted");
                    var event = it.next();
                    switch (event) {
                        case StreamEvent.ContentDelta(String chunk) -> {
                            listener.onContent(chunk);
                            buf.append(chunk);
                        }
                        case StreamEvent.MessageStop() -> {
                            break loop;
                        }
                        default -> {}
                    }
                }
                history.add(new Message("assistant", List.of(new TextContent(buf.toString()))));
            }
        }
    }

    @SneakyThrows
    public static void main(String[] args) {
        var agent = new ClaudeAgent(Files.readString(Constants.CLAUDE_APIKEY_PATH).trim());
        var chat = agent.createChat();
        var listener = new ChatStreamListener() {

            @Override
            public void onThought(String thoughtChunk) {

            }

            @Override
            public void onContent(String contentChunk) {
                System.out.print(contentChunk);
            }
        } ;
        var ctrl = new ChatController() {

            @Override
            public boolean isAborted() {
                return false;
            }
        };
        chat.send("Describe the image", List.of(new File(
                Files.readAllBytes(Path.of("/Users/leen/DeskTop/chris.jpeg")),
                "image/jpeg"
        )), listener, ctrl);
        System.out.println();
        System.out.println();
        chat.send("Give me a brief title for the image", List.of(), listener, ctrl);
    }

}
