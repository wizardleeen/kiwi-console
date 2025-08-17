package org.kiwi.console.app.rest;

import jakarta.annotation.Nullable;

public record AppSearchRequest(
        String name,
        String ownerId,
        int page,
        int pageSize,
        @Nullable String newlyCreatedId

) {
}
