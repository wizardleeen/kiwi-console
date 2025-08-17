package org.kiwi.console.kiwi;

public record GenerationConfig(
        String id,
        String name,
        String model,
        String pageCreatePrompt,
        String pageUpdatePrompt,
        String pageFixPrompt,
        String kiwiCreatePrompt,
        String kiwiUpdatePrompt,
        String kiwiFixPrompt,
        String createAnalyzePrompt,
        String updateAnalyzePrompt,
        String autoTestPrompt,
        String pageTemplateRepo,
        String pageTemplateBranch,
        String kiwiTemplateRepo,
        String kiwiTemplateBranch
) {
}
