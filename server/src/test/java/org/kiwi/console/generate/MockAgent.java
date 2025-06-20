package org.kiwi.console.generate;

class MockAgent implements Agent {

    @Override
    public Chat createChat() {
        return new MockChat();
    }
}
