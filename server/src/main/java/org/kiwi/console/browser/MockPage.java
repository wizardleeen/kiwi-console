package org.kiwi.console.browser;

import org.kiwi.console.generate.PlaywrightActions;

import java.util.UUID;

public class MockPage implements Page {
    @Override
    public String getTargetId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void navigate(String url) {

    }

    @Override
    public void log(String log) {

    }

    @Override
    public byte[] getScreenshot() {
        return new byte[0];
    }

    @Override
    public String getConsoleLogs() {
        return "";
    }

    @Override
    public String getDOM() {
        return "";
    }

    @Override
    public ExecuteResult execute(PlaywrightActions.PlaywrightCommand action) {
        return ExecuteResult.success();
    }

    @Override
    public void close() {

    }
}
