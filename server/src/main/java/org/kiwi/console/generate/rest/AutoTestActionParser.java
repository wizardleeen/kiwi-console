package org.kiwi.console.generate.rest;

import org.kiwi.console.generate.AgentException;
import org.kiwi.console.generate.AutoTestActionType;

public class AutoTestActionParser {

    public static AutoTestAction parse(String text) {
        var reader = new Reader(text);
        if (reader.isEof())
            throw new RuntimeException("Invalid action text: " + text);
        var actionTypeStr = reader.readLine();
        AutoTestActionType type;
        try {
            type = AutoTestActionType.valueOf(actionTypeStr.toUpperCase());
        } catch (Exception e) {
            throw new AgentException("Invalid action type: " + actionTypeStr);
        }
        if (reader.isEof())
            throw new RuntimeException("Invalid action text: " + text);
        var desc = reader.readLine();
        var content =reader.readRemaining();
        return new AutoTestAction(type, desc, content);
    }

    private static class Reader {
        final String text;
        int pos;
        StringBuilder buf = new StringBuilder();

        private Reader(String text) {
            this.text = text;
        }

        String readLine() {
            buf.setLength(0);
            while (!isEof()) {
                var c = text.charAt(pos);
                switch (c) {
                    case '\n' -> {
                        next();
                        return buf.toString();
                    }
                    case '\r' -> {
                        next();
                        if (!isEof() && get() == '\n')
                            next();
                        return buf.toString();
                    }
                    default -> {
                        next();
                        buf.append(c);
                    }
                }
            }
            return buf.toString();
        }

        String readRemaining() {
            buf.setLength(0);
            while (!isEof()) {
                buf.append(get());
                next();
            }
            return buf.toString();
        }

        char get() {
            return text.charAt(pos);
        }

        boolean isEof() {
            return pos >= text.length();
        }

        void next() {
            pos++;
        }

    }

}
