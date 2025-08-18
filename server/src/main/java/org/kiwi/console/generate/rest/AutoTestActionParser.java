package org.kiwi.console.generate.rest;

import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.generate.AutoTestActionType;

import java.util.Objects;

@Slf4j
public class AutoTestActionParser {

    public static AutoTestAction parse(String text) {
        var reader = new Reader(text);
        AutoTestActionType type = null;
        while (!reader.isEof()) {
            var line = reader.readLine();
            try {
                type = AutoTestActionType.valueOf(line.trim().toUpperCase());
                break;
            } catch (Exception e) {
                log.warn("Skipping extraneous line: " + line);
            }
        }
        if (reader.isEof())
            throw new RuntimeException("Invalid action text: " + text);
        var desc = reader.readLine();
        var content = reader.readRemaining();
        return new AutoTestAction(Objects.requireNonNull(type), desc, content);
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
