package org.kiwi.console.patch;

import org.kiwi.console.generate.CorruptPatchException;

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
            if (line.startsWith("@@")) {
                hunks.add(new Hunk(header, buf.toString()));
                header = parseHunkHeader(line);
                buf.setLength(0);
            } else
                buf.append(line).append('\n');
        }
        hunks.add(new Hunk(header, buf.toString()));
        return new Patch(hunks);
    }


    public HunkHeader parseHunkHeader(String line) {
        var parts = line.split(" ");
        if (parts.length != 4 || !parts[0].equals("@@") || !parts[3].equals("@@"))
            throw new CorruptPatchException("Invalid hunk header: " + line);
        var op = switch (parts[1]) {
            case "insert" -> Operation.insert;
            case "delete" -> Operation.delete;
            case "replace" -> Operation.replace;
            default -> throw new CorruptPatchException("Unknown operation: " + parts[1]);
        };
        var liens = parts[2].split(":");
        if (liens.length != 2)
            throw new CorruptPatchException("Invalid line numbers: " + parts[2]);
        try {
            var startLn = Integer.parseInt(liens[0]);
            var endLn = Integer.parseInt(liens[1]);
            return new HunkHeader(op, startLn, endLn);
        } catch (NumberFormatException e) {
            throw new CorruptPatchException("Invalid line numbers: " + parts[2]);
        }
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
