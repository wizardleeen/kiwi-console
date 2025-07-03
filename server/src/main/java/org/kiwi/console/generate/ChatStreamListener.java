package org.kiwi.console.generate;

public interface ChatStreamListener {

    void onThought(String thoughtChunk);

    void onContent(String contentChunk);

}

