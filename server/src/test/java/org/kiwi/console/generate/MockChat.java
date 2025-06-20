package org.kiwi.console.generate;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

class MockChat implements Chat {

    private @Nullable String lastCode;

    @Override
    public void send(String text, ChatStreamListener listener, ChatController ctrl) {
        var prompt = MockPromptParser.parse(text);
        lastCode = switch (prompt.kind()) {
            case PLAN -> "3";
            case CREATE, PAGE_CREATE-> prompt.prompt();
            case UPDATE, PAGE_UPDATE -> "@@ replace 1:10000 @@\n" + prompt.prompt();
            case PAGE_COMMIT_MSG -> "commit front";
            case FIX -> requireNonNull(lastCode).replace("Error", "Fixed");
            case COMMIT_MSG -> "commit";
        };
        listener.onContent(lastCode);
    }


}
