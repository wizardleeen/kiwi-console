package org.kiwi.console.generate;

import javax.annotation.Nullable;

public record Prompt(PromptKind kind, String prompt, @Nullable String code, String apiCode) {
}
