package org.kiwi.console.object.rest;

import javax.annotation.Nullable;
import java.util.Map;

public record SearchRequest(
        String type,
        Map<String, Object> criteria,
        @Nullable String newlyCreatedId,
        int page,
        int pageSize
) {
}
