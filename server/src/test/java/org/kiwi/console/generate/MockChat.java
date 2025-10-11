package org.kiwi.console.generate;

import org.kiwi.console.file.File;

import javax.annotation.Nullable;

import java.util.List;

import static java.util.Objects.requireNonNull;

class MockChat implements Chat {

    private @Nullable String lastCode;

    @Override
    public void send(String text, @Nullable List<File> attachments, ChatStreamListener listener, ChatController ctrl) {
        var prompt = MockPromptParser.parse(text);
        lastCode = switch (prompt.kind()) {
            case CREATE_ANALYZE -> """
                    3
                    Test App
                    """;
            case UPDATE_ANALYZE -> """
                    {
                        "tasks": [
                            {
                                "type": "MODIFY_MODULE",
                                "moduleName": "Kiwi",
                                "suggestion": "change"
                            },
                            {
                                "type": "MODIFY_MODULE",
                                "moduleName": "Web",
                                "suggestion": "change"
                            },
                            {
                                "type": "TEST_MODULE",
                                "moduleName": "Web"
                            }

                        ]
                    }
                    """;
            case CREATE, UPDATE -> "@@ src/main.kiwi @@\n" + prompt.prompt();
            case PAGE_CREATE -> "@@  src/App.tsx @@\n// " + prompt.appName() + prompt.prompt();
            case PAGE_UPDATE -> "@@  src/App.tsx @@\n" + prompt.prompt();
            case PAGE_COMMIT_MSG -> "commit front";
            case AUTO_TEST -> "ACCEPT\n{}";
            case FIX -> requireNonNull(lastCode).replace("Error", "Fixed");
            case COMMIT_MSG -> "commit";
            case DATA -> "echo \"SUCCESSFUL\"";
            case DATA_FIX -> "echo \"FIXED\"";
        };
        listener.onContent(lastCode + "@@@@");
    }


}
