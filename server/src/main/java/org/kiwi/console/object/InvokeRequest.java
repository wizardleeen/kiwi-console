package org.kiwi.console.object;

import java.util.Map;

public record InvokeRequest(
        long appId,
        Object receiver,
        String method,
        Map<String, Object> arguments
) {
}
