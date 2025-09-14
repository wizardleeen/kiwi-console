package org.kiwi.console.browser;

import org.kiwi.console.file.File;

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
    public void hover(String selector) {

    }

    @Override
    public void mouseDown() {

    }

    @Override
    public void mouseUp() {

    }

    @Override
    public void click(String selector) {

    }

    @Override
    public void fill(String selector, String value) {

    }

    @Override
    public void clear(String selector) {

    }

    @Override
    public void press(String selector, String key) {

    }

    @Override
    public void setInputFile(String selector, File file) {

    }

    @Override
    public void dragAndDrop(String selector, String targetSelector) {

    }

    @Override
    public boolean isVisible(String selector) {
        return true;
    }

    @Override
    public boolean isHidden(String selector) {
        return false;
    }

    @Override
    public boolean containText(String selector, String text) {
        return false;
    }


    @Override
    public void close() {

    }
}
