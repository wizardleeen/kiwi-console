package org.kiwi.console.browser;

import org.kiwi.console.file.File;

public interface Page {
    String getTargetId();

    void navigate(String url);

    void reload();

    void log(String log);

    byte[] getScreenshot();

    String getConsoleLogs();

    String getDOM();

    void hover(String selector);

    void mouseDown();

    void mouseUp();

    void click(String selector);

    void fill(String selector, String value);

    void clear(String selector);

    void press(String selector, String key);

    void setInputFile(String selector, File file);

    void dragAndDrop(String selector, String targetSelector);

    boolean isVisible(String selector);

    boolean isHidden(String selector);

    boolean containText(String selector, String text);

    void close();
}
