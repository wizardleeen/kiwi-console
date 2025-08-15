package org.kiwi.console.generate.k2;

public record TextContent(String text) implements Content {
    @Override
    public String getType() {
        return "text";
    }
}
