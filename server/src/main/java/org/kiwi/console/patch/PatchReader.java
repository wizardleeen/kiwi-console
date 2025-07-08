package org.kiwi.console.patch;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

class PatchReader {

    private final String patch;
    private int pos;

    public PatchReader(String patch) {
        this.patch = patch;
    }

    Patch read() {
        var headerLn = readLine();
        if (headerLn == null)
            return new Patch(List.of());
        var header = parseHunkHeader(headerLn);
        var buf = new StringBuilder();
        var hunks = new ArrayList<Hunk>();
        String line;
        while ((line = readLine()) != null) {
            if (isHunkHeader(line)) {
                hunks.add(new Hunk(header, buf.toString()));
                header = parseHunkHeader(line);
                buf.setLength(0);
            } else
                buf.append(line).append('\n');
        }
        hunks.add(new Hunk(header, buf.toString()));
        return new Patch(hunks);
    }

    private boolean isHunkHeader(String line) {
        for (int i = 0; i < line.length(); i++) {
            var c = line.charAt(i);
            switch (c) {
                case ' ', '\t' -> {}
                case '@' -> {
                    return i < line.length() - 1 && line.charAt(i + 1) == '@';
                }
                default -> {
                    return false;
                }
            }
        }
        return false;
    }

    public HunkHeader parseHunkHeader(String line) {
        return new HunkHeaderParser(line).parse();
    }

    private @Nullable String readLine() {
        if (isEof())
            return null;
        var buf = new StringBuilder();
        while (!isEof()) {
            switch (get()) {
                case '\n' -> {
                    next();
                    return buf.toString();
                }
                case '\r' -> {
                    next();
                    if (!isEof() && get() == '\n')
                        next();
                    return buf.toString();
                }
                default -> {
                    buf.append(get());
                    next();
                }
            }
        }
        return buf.toString();
    }

    private char get() {
        return patch.charAt(pos);
    }

    private void next() {
        pos++;
    }

    boolean hasNext() {
        return pos < patch.length();
    }

    boolean isEof() {
        return pos >= patch.length();
    }

    private void skipWhitespaces() {
        while (!isEof() && Character.isWhitespace(get()))
            next();
    }

}
