package org.kiwi.console.patch;

class Lexer {

    private final Tokenizer tokenizer;
    private Token token;

    public Lexer(String text) {
        this.tokenizer = new Tokenizer(text);
        token = tokenizer.nextToken();
    }

    public void next() {
        token = tokenizer.nextToken();
    }

    public Token token() {
        return token;
    }

    public String getText() {
        return tokenizer.getText();
    }

}
