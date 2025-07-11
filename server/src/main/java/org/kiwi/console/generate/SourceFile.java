package org.kiwi.console.generate;

import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record SourceFile(Path path, String content) {

    @SneakyThrows
    public static List<SourceFile> getAllSourceFiles(Path root) {
        try (var s = Files.walk(root.resolve("src"))) {
            var files = new ArrayList<SourceFile>();
            s.forEach(p -> {
                if (p.toFile().isDirectory())
                    return;
                try {
                    files.add(new SourceFile(root.relativize(p), Files.readString(p)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return files;
        }
    }




}
