package org.kiwi.console.generate.k2;

import java.util.List;

public record VideoContent(List<String> video) implements Content {
    @Override
    public String getType() {
        return "video";
    }
}
