package org.kiwi.console.generate.qwen;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Request(
        String model,
        List<Message> messages,
        boolean stream,
        @JsonProperty("enable_thinking") boolean enableThinking

) {
}