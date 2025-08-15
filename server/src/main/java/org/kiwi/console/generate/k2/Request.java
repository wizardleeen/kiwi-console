package org.kiwi.console.generate.k2;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Request(
        String model,
        List<Message> messages,
        @JsonProperty("max_tokens") int maxTokens,
        boolean stream,
        @JsonProperty("enable_thinking") boolean enableThinking

) {
}