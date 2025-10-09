package org.kiwi.console.generate.rest;

import jakarta.annotation.Nullable;

import java.util.List;

public record ExchangeDTO(
        String id,
        String appId,
        String prompt,
        String status,
        @Nullable String productURL,
        @Nullable String managementURL,
        @Nullable String sourceCodeURL,
        @Nullable String errorMessage,
        List<String> attachmentUrls,
        List<ExchangeTaskDTO> tasks,
        String testPageId,
        int chainDepth
) {
}
