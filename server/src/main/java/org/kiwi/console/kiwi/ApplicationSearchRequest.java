package org.kiwi.console.kiwi;

import javax.annotation.Nullable;

public record ApplicationSearchRequest(
        String name,
        String ownerId,
        int page,
        int pageSize,
        @Nullable String newlyChangedId
) {
}
