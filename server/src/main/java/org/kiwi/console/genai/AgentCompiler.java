package org.kiwi.console.genai;

import jakarta.annotation.Nullable;

public interface AgentCompiler {
    DeployResult deploy(long appId, String token, String source);

    @Nullable
    String getCode(long appId);
}
