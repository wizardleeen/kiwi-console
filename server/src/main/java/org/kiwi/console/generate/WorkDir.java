package org.kiwi.console.generate;

import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record WorkDir(Path root) {

    public static WorkDir from(Path baseDir, String projectName) {
        return new WorkDir(baseDir.resolve(projectName));
    }

    Path getSrcPath() {
        return root.resolve("src");
    }

    @SneakyThrows
    void writeSource(Path path, String text) {
        path = root.resolve(path);
        Files.createDirectories(path.getParent());
        Files.writeString(path, text);
    }

    boolean exist(Path path) {
        return Files.exists(root.resolve(path));
    }

    @SneakyThrows
    void removeFile(Path path) {
        Files.delete(root.resolve(path));
    }

    @SneakyThrows
    List<SourceFile> getAllSourceFiles() {
        return SourceFile.getAllSourceFiles(root);
    }

    public InputStream openTargetInput() {
        try {
            return Files.newInputStream(root.resolve("target").resolve("target.mva"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
