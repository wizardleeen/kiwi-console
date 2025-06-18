package org.kiwi.console.genai;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

class MockAgentCompiler implements AgentCompiler {

    private final Map<Long, String> existing = new HashMap<>();

    @Override
    public DeployResult deploy(long appId, String token, String source) {
        if (source.endsWith("-error"))
            return new DeployResult(false, "Compilation failed.");
        existing.put(appId, source);
        return new DeployResult(
                true,
                ""
        );
    }

    @Nullable
    @Override
    public String getCode(long appId) {
        return existing.get(appId);
    }
}
