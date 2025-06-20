package org.kiwi.console.generate;

public interface Chat {

    void send(String text, ChatStreamListener listener, ChatController ctrl);

}
