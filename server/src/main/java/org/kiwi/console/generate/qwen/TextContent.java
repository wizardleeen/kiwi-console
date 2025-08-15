package org.kiwi.console.generate.qwen;

public record TextContent(String text) implements Content {
    @Override
    public String getType() {
        return "text";
    }
}
