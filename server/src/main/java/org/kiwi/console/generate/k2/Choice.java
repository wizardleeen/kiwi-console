package org.kiwi.console.generate.k2;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Choice(Delta delta, int index, @JsonProperty("finish_reason") String finishReason) {
}
