package org.kiwi.console.generate;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public record SourceFile(Path path, String content) {

    public static final Set<Path> ignoredFiles = Set.of(
            Path.of("node_modules"),
            Path.of("target"),
            Path.of("dist"),
            Path.of("src/components/ui"),
            Path.of("package-lock.json")
    );

    @SneakyThrows
    public static List<SourceFile> getAllSourceFiles(Path root) {
        var files = new ArrayList<SourceFile>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                var path = root.relativize(dir);
                return isIgnored(path) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @SneakyThrows
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                log.info("Visiting file {}", file);
                var path = root.relativize(file);
                if (!isIgnored(path))
                    files.add(new SourceFile(path, Files.readString(file)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.error("Failed to visit file: {}", root.relativize(file), exc);
                return FileVisitResult.CONTINUE;
            }

            private boolean isIgnored(Path path) {
                return path.getFileName().toString().startsWith(".") || ignoredFiles.contains(path);
            }

        });
        return files;
    }




}
