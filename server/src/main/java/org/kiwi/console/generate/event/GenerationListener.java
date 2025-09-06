package org.kiwi.console.generate.event;

import org.kiwi.console.generate.rest.ExchangeDTO;

public interface GenerationListener {

    void onThought(String thoughtChunk);

    void onContent(String contentChunk);

    void onProgress(ExchangeDTO exchange);

    default void close() {
    }

}
