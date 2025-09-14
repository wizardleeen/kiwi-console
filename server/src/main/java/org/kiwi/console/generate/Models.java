package org.kiwi.console.generate;

import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.file.File;

import java.util.List;

@Slf4j
public class Models {

    private Models() {
    }

    public static String generateContent(Model model, String prompt, List<File> attachments, AbortController abortController) {
        return generateContent(model.createChat(false), prompt, attachments, abortController);
    }

    public static String generateContent(Chat chat, String prompt, List<File> attachments, AbortController abortController) {
        var buf = new StringBuilder();
        send(chat, prompt, attachments, abortController, buf);
        int endPos;
        while ((endPos = getEndPosition(buf)) == -1) {
            log.info("Continue generation");
            send(chat, "Continue generation", attachments, abortController, buf);
        }
        return buf.substring(0, endPos);
    }

    private static void send(Chat chat, String text, List<File> attachments, AbortController abortController, StringBuilder buf) {
        chat.send(text, attachments, new ChatStreamListener() {
            @Override
            public void onThought(String thoughtChunk) {
            }

            @Override
            public void onContent(String contentChunk) {
                buf.append(contentChunk);
            }
        }, abortController::isAborted);

    }

    private static int getEndPosition(CharSequence output) {
        var i = output.length() - 1;
        loop: while (i >= 0) {
            var c = output.charAt(i);
            switch (c) {
                case '\n', '\r', ' ', '\t' -> i--;
                default -> {
                    break loop;
                }
            }
        }
        if (i >= 3 && output.charAt(i) == '@' && output.charAt(i - 1) == '@' &&
                output.charAt(i - 2) == '@' && output.charAt(i - 3) == '@')
            return i - 3;
        else
            return -1;
    }


}
