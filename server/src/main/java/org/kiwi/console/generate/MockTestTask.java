package org.kiwi.console.generate;

import org.kiwi.console.kiwi.Tech;

import java.util.UUID;

public class MockTestTask implements TestTask {

    private final CodeAgentListener listener;

    public MockTestTask(CodeAgentListener listener) {
        this.listener = listener;
    }

    @Override
    public TestResult runTest() {
        listener.onAttemptStart();
        listener.onAttemptSuccess();
        return TestResult.ACCEPTED;
    }

    protected String nextTargetId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Tech getTech() {
        return Tech.WEB;
    }

    @Override
    public void close() {

    }
}
