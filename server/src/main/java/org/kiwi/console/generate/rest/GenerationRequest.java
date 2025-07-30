package org.kiwi.console.generate.rest;

import java.util.List;

public record GenerationRequest(
        String appId,
        String prompt,
        List<String> attachmentUrls,
        boolean skipPageGeneration) {

    public static GenerationRequest create(String appId, String prompt) {
        return new GenerationRequest(appId, prompt, List.of(), false);
    }

}
