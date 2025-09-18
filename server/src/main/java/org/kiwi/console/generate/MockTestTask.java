package org.kiwi.console.generate;

import org.kiwi.console.kiwi.Tech;

import java.util.UUID;

public class MockTestTask implements TestTask {

    @Override
    public TestResult runTest() {
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
