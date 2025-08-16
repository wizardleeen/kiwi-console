package org.kiwi.console.object.rest;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record InvokeRequest(
        @NotNull
        Object receiver,
        @NotNull
        String method,
        @NotNull
        Map<String, Object> arguments
) {
}
