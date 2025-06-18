package org.kiwi.console.genai;

class MockAgent implements Agent {

    @Override
    public Chat createChat() {
        return new MockChat();
    }
}
