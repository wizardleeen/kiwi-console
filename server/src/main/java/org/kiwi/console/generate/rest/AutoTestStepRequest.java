package org.kiwi.console.generate.rest;

import java.util.List;

public record AutoTestStepRequest(
        String exchangeId,
        List<String> attachmentUrls
) {
}
