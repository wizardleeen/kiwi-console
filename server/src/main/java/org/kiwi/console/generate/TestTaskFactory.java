package org.kiwi.console.generate;

import org.kiwi.console.kiwi.Tech;

import java.util.function.Consumer;

public interface TestTaskFactory {

    TestTask createTestTask(
            long appId,
            String projectName,
            String url,
            Model model,
            String promptTemplate,
            String requirement,
            ModuleRT module,
            AbortController abortController,
            Consumer<String> setTargetId
    );

    Tech getTech();
}
