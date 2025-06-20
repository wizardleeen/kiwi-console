package org.kiwi.console.generate;

import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public record WorkDir(Path path) {

    public static WorkDir from(Path baseDir, long appId) {
        return new WorkDir(baseDir.resolve(Long.toString(appId)));
    }

    public void init() {
        try {
            if (!path.toFile().exists()) {
                Files.createDirectory(path);
                Files.createDirectory(getSrcPath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Path getSrcPath() {
        return path.resolve("src");
    }

    @SneakyThrows
    void writeSource(String fileName, String text) {
        Files.writeString(getSrcPath().resolve(fileName), text);
    }

    public InputStream openTargetInput() {
        try {
            return Files.newInputStream(path.resolve("target").resolve("target.mva"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
