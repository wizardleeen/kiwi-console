package org.kiwi.console.kiwi;

public record ExchangeSearchRequest(
        String prompt,
        String appId,
        String userId,
        boolean includeChildren,
        int page,
        int pageSize
) {
}
