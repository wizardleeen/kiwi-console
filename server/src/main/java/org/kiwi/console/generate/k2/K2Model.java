package org.kiwi.console.generate.k2;

import lombok.SneakyThrows;
import org.kiwi.console.file.File;
import org.kiwi.console.generate.*;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class K2Model implements Model {

    private final K2Client client;

    public K2Model(String apikey) {
        this.client = new K2Client(apikey);
    }

    @Override
    public Chat createChat(boolean outputThought) {
        return new k2Chat();
    }

    @Override
    public String getName() {
        return "k2";
    }

    private class k2Chat implements Chat {

        private final List<Message> history = new ArrayList<>();

        @Override
        public void send(String text, List<File> attachments, ChatStreamListener listener, ChatController ctrl) {
            var contents = new ArrayList<Content>();
            contents.add(new TextContent(text));
            for (File attachment : attachments) {
                var mimeType = attachment.mimeType();
                if (mimeType.startsWith("text/") || mimeType.equals("application/json"))
                    contents.add(new TextContent(new String(attachment.bytes(), StandardCharsets.UTF_8)));
                else
                    throw new BusinessException(ErrorCode.UNSUPPORTED_ATTACHMENT, attachment.mimeType());
            }
            var msg = new Message("user", contents);
            history.add(msg);
            var respContent = new StringBuilder();
            try (var stream = client.send(history)) {
                var it = stream.iterator();
                while (it.hasNext()) {
                    if (ctrl.isAborted())
                        throw new AgentException("Aborted");
                    var resp = it.next();
                    resp.choices().stream()
                            .filter(c -> c.index() == 0)
                            .findFirst()
                            .ifPresent(firstChoice -> {
                                var chunk = firstChoice.delta().content();
                                if (chunk != null) {
                                    listener.onContent(chunk);
                                    respContent.append(chunk);
                                }
                            });
                }
            }
            history.add(new Message("assistant", List.of(new TextContent(respContent.toString()))));
        }
    }

    @SneakyThrows
    public static void main(String[] args) {
        var agent = new K2Model(Files.readString(Path.of("/Users/leen/develop/k2_apikey")).trim());
        var chat = agent.createChat(false);
        var l = new ChatStreamListener() {

            @Override
            public void onThought(String thoughtChunk) {

            }

            @Override
            public void onContent(String contentChunk) {
                System.out.print(contentChunk);
            }
        };
        var ctrl = new ChatController() {
            @Override
            public boolean isAborted() {
                return false;
            }
        };
        chat.send("I'm Leen", List.of(), l, ctrl);
        System.out.println();
        chat.send("Who am I", List.of(), l, ctrl);
        System.out.println();
        System.out.println();
        System.out.println("Done");
    }

}
