package org.kiwi.console.patch;

import org.kiwi.console.generate.MalformedHunkException;

class Tokenizer {
    private final String text;
    private int pos;
    private char ch;
    public static final int EOI = 0x1A;

    Tokenizer(String text) {
        this.text = text;
        next();
    }

    Token nextToken() {
        var startPos = pos;
        return switch (get()) {
            case '@' -> {
                next();
                if (get() != '@')
                    throw new MalformedHunkException(text, pos, "Expected '@' after '@' in hunk header, but found: '" + get() + "'");
                next();
                yield new Token(TokenKind.AT_AT, startPos, "@@");
            }
            case ' ', '\t' -> {
                while (get() == ' ' || get() == '\t')
                    next();
                yield nextToken();
            }
            case ':' -> {
                next();
                yield new Token(TokenKind.COLON, startPos,  ":");
            }
            case EOI -> new Token(TokenKind.EOF, startPos, "");
            default -> throw new MalformedHunkException(text, startPos, "Unexpected character: '" + get() + "'");
        };
    }

    private Token nextIdent() {
        var startPos = pos;
        var buf = new StringBuilder();
        while (Character.isLetter(get())) {
            buf.append(get());
            next();
        }
        return new Token(TokenKind.IDENTIFIER, startPos, buf.toString());
    }

    char get() {
        return ch;
    }

    void next() {
        ch = pos < text.length() ? text.charAt(pos++) : EOI;
    }

    boolean isEof() {
        return ch == EOI;
    }

    public String getText() {
        return text;
    }

}
