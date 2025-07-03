package org.kiwi.console.generate;

public class LineNumAnnotator {

    public static String annotate(String text) {
        var line = 1;
        var buf = new StringBuilder("1   | ");
        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            switch (c) {
                case '\n' -> {
                    buf.append('\n');
                    line++;
                    appendLineAnnotation(buf, line);
                }
                case '\r' -> {
                    if (i + i < text.length() && text.charAt(i + 1) == '\n')
                        i++;
                    buf.append('\n');
                    line++;
                    appendLineAnnotation(buf, line);
                }
                default -> buf.append(c);
            }
        }
        return buf.toString();
    }

    private static void appendLineAnnotation(StringBuilder buf, int line) {
        buf.append(line);
        if (line >= 1000)
            buf.append("| ");
        else if (line >= 100)
            buf.append(" | ");
        else if (line >= 10)
            buf.append("  | ");
        else
            buf.append("   | ");
    }

}
