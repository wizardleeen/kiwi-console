package org.kiwi.console.generate.qwen;

import java.util.List;

public record VideoContent(List<String> video) implements Content {
    @Override
    public String getType() {
        return "video";
    }
}
