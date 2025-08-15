package org.kiwi.console.generate.qwen;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Choice(Delta delta, int index, @JsonProperty("finish_reason") String finishReason) {
}
