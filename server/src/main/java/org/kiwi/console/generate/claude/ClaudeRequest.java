package org.kiwi.console.generate.claude;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
record ClaudeRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        Thinking thinking,
        List<Message> messages,
        Boolean stream
) {}

record Thinking(String type, @JsonProperty("budget_tokens") int budgetTokens) {

}

record ClaudeResponse(
        String id,
        String type,
        String role,
        List<ContentBlock> content,
        String model,
        @JsonProperty("stop_reason") String stopReason,
        @JsonProperty("stop_sequence") String stopSequence,
        Usage usage
) {}

record Usage(
        @JsonProperty("input_tokens") int inputTokens,
        @JsonProperty("output_tokens") int outputTokens
) {}

record ContentBlock(
        String type,
        String text
) {}

record Message(
        String role,
        Object content
) {
    public Message(String role, String text) {
        this(role, (Object) text);
    }

    public Message(String role, List<Content> contentList) {
        this(role, (Object) contentList);
    }
}

sealed interface Content permits TextContent, ImageContent {}

record TextContent(
        String type,
        String text
) implements Content {
    public TextContent(String text) {
        this("text", text);
    }
}

record ImageContent(
        String type,
        ImageSource source
) implements Content {
    public ImageContent(ImageSource source) {
        this("image", source);
    }
}

record ImageSource(
        String type,
        @JsonProperty("media_type") String mediaType,
        String data
) {}

record ImageData(
        String mediaType,
        String data
) {
    public static ImageData jpeg(String base64Data) {
        return new ImageData("image/jpeg", base64Data);
    }

    public static ImageData png(String base64Data) {
        return new ImageData("image/png", base64Data);
    }

    public static ImageData webp(String base64Data) {
        return new ImageData("image/webp", base64Data);
    }

    public static ImageData gif(String base64Data) {
        return new ImageData("image/gif", base64Data);
    }
}

record StreamResponse(
        String type,
        Delta delta,
        StreamMessage message
) {}

record Delta(String text) {}

record StreamMessage(
        String id,
        String type,
        String role,
        List<Object> content,
        String model,
        Usage usage
) {}

sealed interface StreamEvent {
    record MessageStart() implements StreamEvent {}
    record ContentStart() implements StreamEvent {}
    record ContentDelta(String text) implements StreamEvent {}
    record ContentStop() implements StreamEvent {}
    record MessageStop() implements StreamEvent {}
    record Done() implements StreamEvent {}
}