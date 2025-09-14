package org.kiwi.console.generate;

import lombok.SneakyThrows;
import org.kiwi.console.browser.Page;

public interface TestAgent {
    @SneakyThrows
    TestResult runTest(
            long appId,
            Page page,
            Model model,
            String promptTemplate,
            String requirement,
            AbortController abortController
    );
}
