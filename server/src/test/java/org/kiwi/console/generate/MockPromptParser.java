package org.kiwi.console.generate;

import java.util.ArrayList;

public class MockPromptParser {

    public static Prompt parse(String text) {
        var parts = new PromptReader(text).readParts();
        if (parts.length == 0)
            throw invalidPrompt(text);
        var kind = parts[0].trim();
        switch (kind) {
            case "create-analyze" -> {
                if (parts.length < 2)
                    throw invalidPrompt(text);
                return new Prompt(PromptKind.CREATE_ANALYZE, parts[1], null, null);
            }
            case "update-analyze" -> {
                if (parts.length < 4)
                    throw invalidPrompt(text);
                return new Prompt(PromptKind.UPDATE_ANALYZE, parts[1], parts[2], parts[3]);
            }
            case "create" -> {
                if (parts.length < 2)
                    throw invalidPrompt(text);
                return new Prompt(PromptKind.CREATE, parts[1], null, null);
            }
            case "update" -> {
                if (parts.length < 3)
                    throw invalidPrompt(text);
                return new Prompt(PromptKind.UPDATE, parts[1], parts[2], null);
            }
            case "page_create" -> {
                if (parts.length < 3)
                    throw invalidPrompt(text);
                return new Prompt(PromptKind.PAGE_CREATE, parts[1], null, parts[2]);
            }
            case "page_update" -> {
                if (parts.length < 4)
                    throw invalidPrompt(text);
                return new Prompt(PromptKind.PAGE_UPDATE, parts[1], parts[2], parts[3]);
            }
            case "fix" -> {
                if (parts.length < 2)
                    throw invalidPrompt(text);
                return new Prompt(PromptKind.FIX, parts[1], null, null);
            }
            case "commit_msg" -> {
                return new Prompt(PromptKind.COMMIT_MSG, null, null, null);
            }
            case "page_commit_msg" -> {
                return new Prompt(PromptKind.PAGE_COMMIT_MSG, null, null, null);
            }
            default -> throw invalidPrompt(text);
        }
    }

    static class PromptReader {

        private final String prompt;
        private int pos;


        PromptReader(String prompt) {
            this.prompt = prompt;
        }

        String[] readParts() {
            var parts = new ArrayList<String>();
            var buf = new StringBuilder();
            while (!isEof()) {
                switch (get()) {
                    case '\r', '\n' -> {
                        skipLineBreaker();
                        buf.append('\n');
                        if (skipLineBreaker()) {
                            parts.add(buf.toString());
                            buf.setLength(0);
                        }
                    }
                    default -> {
                        buf.append(get());
                        next();
                    }
                }
            }
            if (!buf.isEmpty())
                parts.add(buf.toString());
            return parts.toArray(String[]::new);
        }

        private void next() {
            pos++;
        }

        private boolean isEof() {
            return pos >= prompt.length();
        }

        private char get() {
            return prompt.charAt(pos);
        }

        boolean skipLineBreaker() {
            if (isEof())
                return false;
            switch (get()) {
                case '\n' -> {
                    next();
                    return true;
                }
                case '\r' -> {
                    next();
                    if (!isEof() && get() == '\n')
                        next();
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }

    }

    private static IllegalArgumentException invalidPrompt(String text) {
        return new IllegalArgumentException("Invalid prompt: " + text);
    }

}
