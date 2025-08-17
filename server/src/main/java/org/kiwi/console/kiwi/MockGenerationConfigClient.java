package org.kiwi.console.kiwi;

import org.kiwi.console.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MockGenerationConfigClient implements GenerationConfigClient {
    private final GenerationConfig preset;
    private final Map<String, GenerationConfig> configs = new HashMap<>();

    public MockGenerationConfigClient() {
        preset = new GenerationConfig(
                UUID.randomUUID().toString(),
                "default",
                "mock",
                Utils.loadResource("/prompt/page-create.md"),
                Utils.loadResource("/prompt/page-update.md"),
                Utils.loadResource("/prompt/page-fix.md"),
                Utils.loadResource("/prompt/kiwi-create.md"),
                Utils.loadResource("/prompt/kiwi-update.md"),
                Utils.loadResource("/prompt/kiwi-fix.md"),
                Utils.loadResource("/prompt/create-analyze.md"),
                Utils.loadResource("/prompt/update-analyze.md"),
                Utils.loadResource("/prompt/auto-test.md"),
                "",
                "",
                "",
                ""
                );
        configs.put(preset.id(), preset);
    }

    public String getPresetId() {
        return preset.id();
    }

    @Override
    public GenerationConfig get(String id) {
        var config = configs.get(id);
        if (config == null)
            throw new IllegalArgumentException("GenerationConfig not found for id: " + id);
        return copy(config);
    }

    public String save(GenerationConfig config) {
        var copy = copy(config);
        configs.put(copy.id(), copy);
        return copy.id();
    }

    public GenerationConfig copy(GenerationConfig config) {
        return new GenerationConfig(
                config.id() != null ? config.id() :UUID.randomUUID().toString(),
                config.name(),
                config.model(),
                config.pageCreatePrompt(),
                config.pageUpdatePrompt(),
                config.pageFixPrompt(),
                config.kiwiCreatePrompt(),
                config.kiwiUpdatePrompt(),
                config.kiwiFixPrompt(),
                config.createAnalyzePrompt(),
                config.updateAnalyzePrompt(),
                config.autoTestPrompt(),
                config.pageTemplateRepo(),
                config.pageTemplateBranch(),
                config.kiwiTemplateRepo(),
                config.kiwiTemplateBranch()
        );
    }


}
