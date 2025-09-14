package org.kiwi.console.generate;

import org.kiwi.console.browser.Page;

public class MockTestAgent implements TestAgent {
    @Override
    public TestResult runTest(long appId, Page page, Model model, String promptTemplate, String requirement, AbortController abortController) {
        return TestResult.ACCEPTED;
    }
}
