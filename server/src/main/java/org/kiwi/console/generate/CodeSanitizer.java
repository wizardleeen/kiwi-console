package org.kiwi.console.generate;

public class CodeSanitizer {

    public static String sanitizeCode(String text) {
        text = skipExplanatoryText(text);
        if (!text.startsWith("```"))
            return text;
        var i = 0;
        var len = text.length();
        while (i < len) {
            var c = text.charAt(i++);
            if (c == '\n')
                break;
            else if (c == '\r') {
                if (i < len - 1 && text.charAt(i + 1) == '\n')
                    i++;
                break;
            }
        }
        var buf = new StringBuilder();
        var end = text.lastIndexOf("```");
        if (end == -1)
            end = len;
        while (i < end) {
            buf.append(text.charAt(i++));
        }
        return buf.toString();
    }

    private static String skipExplanatoryText(String text) {
        if (isCodeStart(text, 0))
            return text;
        for (int i = 0; i < text.length();) {
            if (isCodeStart(text, i))
                return text.substring(i);
            while (i < text.length()) {
                var c = text.charAt(i++);
                if (c == '\n')
                    break;
                else if (c == '\r') {
                    if (i < text.length() && text.charAt(i) == '\n')
                        i++;
                    break;
                }
            }
        }
        return "";
    }

    private static boolean isCodeStart(String text, int offset) {
        var token = readToken(text, offset);
        return switch (token) {
            case "import", "package", "pub", "prot", "priv", "class",
                    "interface", "enum", "value", "fn", "function",
                    "const", "let", "export", "{" -> true;
            default -> token.startsWith("```") || token.startsWith("//") || token.startsWith("@@");
        };
    }

    private static String readToken(String str, int offset) {
        var i = offset;
        for (; i < str.length() && isWhitespace(str.charAt(i)); i++);
        var buf = new StringBuilder();
        for (; i < str.length(); i++) {
            var c = str.charAt(i);
            switch (c) {
                case ' ', '\t', '\f', '\r', '\n' -> {
                    return buf.toString();
                }
                default -> buf.append(c);
            }
        }
        return buf.toString();
    }

    private static boolean isWhitespace(int ch) {
        return switch (ch) {
            case ' ', '\t', '\f' -> true;
            default -> false;
        };
    }

}
