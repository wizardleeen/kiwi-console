package org.kiwi.console.kiwi;

import javax.annotation.Nullable;

public record GenerationConfig(
        String id,
        String name,
        String pageCreatePrompt,
        String pageUpdatePrompt,
        String pageFixPrompt,
        @Nullable String templateRepo
) {
}
