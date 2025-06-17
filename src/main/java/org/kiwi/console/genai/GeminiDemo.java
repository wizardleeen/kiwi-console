package org.kiwi.console.genai;


import com.google.genai.Client;
import com.google.genai.types.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Scanner;

public class GeminiDemo {

    public static final String model = "gemini-2.5-flash-preview-05-20";

    public static final String apikeyPath = "/Users/leen/develop/gemini/apikey";

    public static void main(String[] args) throws IOException {
        var apikey = Files.readString(Path.of(apikeyPath));
        Client client = Client.builder()
                .apiKey(apikey)
                .build();
        var scanner = new Scanner(System.in);
        var chat = client.chats.create(model, GenerateContentConfig.builder()
                .thinkingConfig(
                        ThinkingConfig.builder()
                                .includeThoughts(true)
                                .build()
                )
                .build());
        for (; ; ) {
            System.out.print("Input: ");
            var msg = scanner.nextLine();
            boolean firstThought = true;
            boolean firstAnswer = true;
            try (var s = chat.sendMessageStream(msg)) {
                for (var resp : s) {
                    for (Part part : Objects.requireNonNull(resp.parts())) {
                        if (part.thought().orElse(false)) {
                            if (firstThought) {
                                firstThought = false;
                                System.out.println("----Thought----");
                            }
                            System.out.println(part.text().orElse(""));
                        } else {
                            if (firstAnswer) {
                                firstAnswer = false;
                                System.out.println("----Answer----");
                            }
                            System.out.println(part.text().orElse(""));
                        }
                    }
                }
            }
        }
    }

}
