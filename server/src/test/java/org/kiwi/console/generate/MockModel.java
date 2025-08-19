package org.kiwi.console.generate;

class MockModel implements Model {

    @Override
    public Chat createChat(boolean outputThought) {
        return new MockChat();
    }

    @Override
    public String getName() {
        return "mock";
    }
}
