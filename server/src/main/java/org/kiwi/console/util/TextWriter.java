package org.kiwi.console.util;

public class TextWriter {

    private int indents;
    private boolean newLine;
    private final StringBuilder sb = new StringBuilder();

    public void indent() {
        indents++;
    }

    public void deIndent() {
        indents--;
    }

    public void writeln(String s) {
        write(s);
        writeln();
    }

    public void write(String s) {
        if (newLine) {
            appendIndents();
            newLine = false;
        }
        for (int i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            sb.append(c);
            if (c == '\n') {
                if (i < s.length() - 1)
                    appendIndents();
                else {
                    newLine = true;
                    break;
                }
            }
        }
    }

    private void appendIndents() {
        sb.append("    ".repeat(Math.max(0, indents)));
    }

    public void writeln() {
        sb.append('\n');
        newLine = true;
    }

    @Override
    public String toString() {
        return sb.toString();
    }

}
