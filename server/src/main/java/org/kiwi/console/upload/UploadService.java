package org.kiwi.console.upload;

import lombok.SneakyThrows;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class UploadService {

    private final Path uploadDir;

    public UploadService(Path uploadDir) {
        this.uploadDir = uploadDir;
    }

    @SneakyThrows
    public String upload(long appId, String fileName, InputStream input) {
        fileName = convertFileName(fileName);
        var path = uploadDir.resolve(Long.toString(appId)).resolve(fileName);
        Files.createDirectories(path.getParent());
        Files.copy(input, path);
        return "/uploads/" + fileName;
    }

    private String convertFileName(String fileName) {
        var lastIdx = fileName.lastIndexOf('.');
        var ext = fileName.substring(lastIdx);
        return UUID.randomUUID() + ext;
    }

}
