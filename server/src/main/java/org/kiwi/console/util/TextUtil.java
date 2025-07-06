package org.kiwi.console.util;

public class TextUtil {

    public static String indent(String text) {
        return indent(text, 4);
    }

    public static String indent(String text, int indents) {
        if (text.isEmpty())
            return "";
        var buf = new StringBuilder();
        for (int i = 0; i < indents; i++) {
            buf.append(' ');
        }
        for (int i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            switch (c) {
                case '\r':
                    if (i < text.length() - 1 && text.charAt(i + 1) == '\n')
                        i++;
                case '\n':
                    buf.append('\n');
                    if (i < text.length() - 1) {
                        for (int j = 0; j < indents; j++) {
                            buf.append(' ');
                        }
                    }
                    break;
                default:
                    buf.append(c);
            }
        }
        return buf.toString();
    }

}
