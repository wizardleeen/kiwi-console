package org.kiwi.console.genai;

import javax.annotation.Nullable;
import java.util.Objects;

class MockChat implements Chat {

    private @Nullable String lastCode;

    @Override
    public String send(String text) {
        var prompt = PromptParser.parse(text);
        return lastCode = switch (prompt.kind()) {
            case CREATE -> prompt.prompt();
            case UPDATE -> Objects.requireNonNull(prompt.code()) + "-" + prompt.prompt();
            case FIX -> Objects.requireNonNull(lastCode) + "-fixed";
        };
    }


}
