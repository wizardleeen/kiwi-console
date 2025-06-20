package org.kiwi.console.generate;

public class Format {

    public static String format(String template, Object...args) {
        if (args.length == 0)
            return template;
        var sb = new StringBuilder();
        var j = 0;
        int i = 0;
        while (i < template.length()) {
            if (template.charAt(i) == '{' && i < template.length() - 1 && template.charAt(i + 1) == '}') {
                sb.append(args[j]);
                i += 2;
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

}
