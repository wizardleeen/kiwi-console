package org.kiwi.console.patch;

enum TokenKind {
    AT_AT {
        @Override
        public String toString() {
            return "'@@'";
        }
    },
    IDENTIFIER,
    EOF {
        @Override
        public String toString() {
            return "<EOF>";
        }
    },
    INTEGER,
    COLON {
        @Override
        public String toString() {
            return "':'";
        }
    },

    ;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
