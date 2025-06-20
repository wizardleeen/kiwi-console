package org.kiwi.console.generate.rest;

public record GenerationRequest(String appId, String prompt, boolean skipPageGeneration) {
}
