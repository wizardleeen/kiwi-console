package org.kiwi.console.generate.data;

import org.kiwi.console.generate.AbortController;
import org.kiwi.console.generate.CodeAgentListener;
import org.kiwi.console.generate.Model;

public record DataManipulationRequest(
        String promptTemplate,
        String fixPromptTemplate,
        long appId,
        String requirement,
        String code,
        CodeAgentListener listener,
        Model model,
        AbortController abortController
) {
}
