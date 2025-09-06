package org.kiwi.console.browser;

import org.kiwi.console.generate.PlaywrightActions;

public interface Page {
    String getTargetId();

    void navigate(String url);

    void log(String log);

    byte[] getScreenshot();

    String getConsoleLogs();

    String getDOM();

    ExecuteResult execute(PlaywrightActions.PlaywrightCommand action);

    void close();
}
