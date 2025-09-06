package org.kiwi.console.browser;

public class MockBrowser implements Browser {
    @Override
    public Page createPage() {
        return new MockPage();
    }

}
