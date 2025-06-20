package org.kiwi.console.util;

public class Format {

    public static String format(String template, Object...params) {
        var buf = new StringBuilder();
        for (int i = 0, j = 0; i < template.length(); i++) {
            var c = template.charAt(i);
            if (c == '{' && i + 1 < template.length() && j < params.length
                    && template.charAt(i + 1) == '}') {
                i++;
                buf.append(params[j++]);
            }
            else
                buf.append(c);
        }
        return buf.toString();
    }

}
