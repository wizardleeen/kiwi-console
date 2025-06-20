package org.kiwi.console.generate.event;

import org.kiwi.console.kiwi.Exchange;

public interface GenerationListener {

    void onThought(String thoughtChunk);

    void onContent(String contentChunk);

    void onProgress(Exchange exchange);

    default void close() {
    }

}
