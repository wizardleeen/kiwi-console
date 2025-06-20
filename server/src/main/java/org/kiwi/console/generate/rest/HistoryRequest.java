package org.kiwi.console.generate.rest;

public record HistoryRequest(
        String appId,
        int page,
        int pageSize
) {
}
