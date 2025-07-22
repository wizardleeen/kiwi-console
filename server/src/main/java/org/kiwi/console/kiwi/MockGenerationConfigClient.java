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
                Utils.loadResource("/prompt/page-create.md"),
                Utils.loadResource("/prompt/page-update.md"),
                Utils.loadResource("/prompt/page-fix.md"),
                null
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
                config.pageCreatePrompt(),
                config.pageUpdatePrompt(),
                config.pageFixPrompt(),
                config.templateRepo()
        );
    }


}
