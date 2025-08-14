package org.kiwi.console.generate.claude;

import org.kiwi.console.util.Constants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ClaudeClientExample {
    
    public static void main(String[] args) throws Exception {
        var apiKey = Files.readString(Constants.CLAUDE_APIKEY_PATH);

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Please set the ANTHROPIC_API_KEY environment variable");
            System.exit(1);
        }
        
        var client = new ClaudeClient(apiKey);
        
        try {
            streamingResponse(client);

            sendImage(client);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void streamingResponse(ClaudeClient client) throws Exception {
        System.out.println("\n=== Streaming Response ===");
        
        client.clearHistory();
        
        var latch = new CountDownLatch(1);
        var responseBuilder = new StringBuilder();
        
        var stream = client.streamMessage(
                "Write a haiku about programming."
        );
        stream.forEach(event -> {
                switch (event) {
                    case StreamEvent.MessageStart() ->
                        System.out.println("Claude is thinking...");
                    case StreamEvent.ContentDelta(var text) -> {
                        System.out.print(text);
                        responseBuilder.append(text);
                    }
                    case StreamEvent.Done() -> {
                        System.out.println("\n[Response complete]");
                        latch.countDown();
                    }
                    default -> {} // Handle other events as needed
                }
        });
        
        latch.await(); // Wait for streaming to complete
        System.out.println("Full response length: " + responseBuilder.length() + " characters");
    }

    private static void sendImage(ClaudeClient client) throws Exception {
        System.out.println("\n=== Streaming Response ===");

        client.clearHistory();

        var latch = new CountDownLatch(1);
        var responseBuilder = new StringBuilder();

        var stream = client.streamMessage(
                "Describe the picture",
                List.of(
                        new ImageData(
                                "image/jpeg",
                                Base64.getEncoder().encodeToString(
                                        Files.readAllBytes(Path.of("/Users/leen/DeskTop/chris.jpeg"))
                                )
                        )
                )
        );
        stream.forEach(event -> {
            switch (event) {
                case StreamEvent.MessageStart() ->
                        System.out.println("Claude is thinking...");
                case StreamEvent.ContentDelta(var text) -> {
                    System.out.print(text);
                    responseBuilder.append(text);
                }
                case StreamEvent.Done() -> {
                    System.out.println("\n[Response complete]");
                    latch.countDown();
                }
                default -> {} // Handle other events as needed
            }
        });

        latch.await(); // Wait for streaming to complete
        System.out.println("Full response length: " + responseBuilder.length() + " characters");
    }


}