package org.kiwi.console.object;

import java.util.List;

public record MultiGetRequest(
        long appId,
        List<String> ids,
        boolean excludeChildren,
        boolean excludeFields
) {
}
