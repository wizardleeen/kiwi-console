package org.kiwi.console.patch;

import org.kiwi.console.generate.MalformedHunkException;
import org.kiwi.console.generate.SourceFile;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PatchReader {

    private final String patch;
    private int pos;

    public PatchReader(String patch) {
        this.patch = patch;
    }

    public Patch read() {
        var headerLn = readLine();
        while (headerLn != null && !headerLn.trim().startsWith("@@"))
            headerLn = readLine();
        if (headerLn == null)
            return new Patch(List.of(), List.of());
        var header = parseHeader(headerLn);
        var buf = new StringBuilder();
        var files = new ArrayList<SourceFile>();
        var removeFiles = new ArrayList<Path>();
        String line;
        while ((line = readLine()) != null) {
            if (isHeader(line)) {
                if (header.removal)
                    removeFiles.add(header.path);
                else
                    files.add(new SourceFile(header.path, buf.toString()));
                header = parseHeader(line);
                buf.setLength(0);
            } else
                buf.append(line).append('\n');
        }
        if (header.removal)
            removeFiles.add(header.path);
        else
            files.add(new SourceFile(header.path, buf.toString()));
        return new Patch(files, removeFiles);
    }

    private boolean isHeader(String line) {
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

    private record Header(Path path, boolean removal) {}

    private Header parseHeader(String line) {
        line = line.trim();
        if (line.length() <= 4 || !line.startsWith("@@") || !line.endsWith("@@"))
            throw new MalformedHunkException(line, 0);
        var text = line.substring(2, line.length() - 2).trim();
        if(text.startsWith("--"))
            return new Header(Path.of(text.substring(2).trim()), true);
        else
            return new Header(Path.of(text.trim()), false);
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

    public static String buildCode(List<SourceFile> sourceFiles) {
        var buf = new StringBuilder();
        for (SourceFile file : sourceFiles) {
            buf.append("@@ ").append(file.path().toString()).append(" @@\n");
            buf.append(file.content()).append('\n');
        }
        return buf.toString();
    }

}
