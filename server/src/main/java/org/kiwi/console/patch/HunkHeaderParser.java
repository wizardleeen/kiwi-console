package org.kiwi.console.patch;

import org.kiwi.console.generate.MalformedHunkException;

class HunkHeaderParser {

    private final Lexer lexer;

    public HunkHeaderParser(String text) {
        lexer = new Lexer(text);
    }

    public HunkHeader parse() {
        accept(TokenKind.AT_AT);
        var op = operation();
        var startLine = integer();
        accept(TokenKind.COLON);
        var endLine = integer();
        accept(TokenKind.AT_AT);
        return new HunkHeader(op, startLine, endLine);
    }

    private Operation operation() {
        var token = accept(TokenKind.IDENTIFIER);
        return switch (token.text()) {
            case "insert" -> Operation.insert;
            case "replace" -> Operation.replace;
            case "delete" -> Operation.delete;
            default -> throw new MalformedHunkException(getText(), token.position(), "Unrecognized operation '" + token.text() + "'");
        };
    }

    private int integer() {
        if (token().kind() == TokenKind.INTEGER) {
            var pos = token().position();
            try {
                var i = Integer.parseInt(token().text());
                nextToken();
                return i;
            }
            catch (NumberFormatException e) {
                throw new MalformedHunkException(getText(), pos, "Invalid integer: " + token().text());
            }
        }
        else
            throw new MalformedHunkException(getText(), token().position(), "Expected integer, found: " + token().kind());
    }

    private Token accept(TokenKind tokenKind) {
        if (token().kind() == tokenKind) {
            var t = token();
            nextToken();
            return t;
        } else
            throw new MalformedHunkException(getText(), token().position(), "Expected token: " + tokenKind + ", found: " + token().kind());
    }

    private Token token() {
        return lexer.token();
    }

    private void nextToken() {
        lexer.next();
    }

    private String getText() {
        return lexer.getText();
    }

}
