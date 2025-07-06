package org.kiwi.console.generate;

public class Format {

    public static String formatKeyed(String template, String key, Object...args) {
        if (args.length == 0)
            return template;
        var sb = new StringBuilder();
        var j = 0;
        int i = 0;
        while (i < template.length()) {
            if (isPlaceholder(template, i, key)) {
                sb.append(args[j]);
                i += 2 + key.length();
                if (++j >= args.length)
                    break;
            }
            else
                sb.append(template.charAt(i++));
        }
        while (i < template.length())
            sb.append(template.charAt(i++));
        return sb.toString();
    }

    private static boolean isPlaceholder(String template, int i, String key) {
        var keyLen = key.length();
        if (template.charAt(i) == '{' && i + keyLen + 1 < template.length() && template.charAt(i + keyLen + 1) == '}') {
            for (int j = 0; j < keyLen; j++) {
                if (template.charAt(i + 1 + j) != key.charAt(j))
                    return false;
            }
            return true;
        }
        else
            return false;
    }

    public static String format(String template, Object...args) {
        return formatKeyed(template, "", args);
    }

}
