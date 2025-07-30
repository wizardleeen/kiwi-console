package org.kiwi.console.file;

import lombok.SneakyThrows;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class FileService {

    private final Path uploadDir;

    public FileService(Path uploadDir) {
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

    @SneakyThrows
    public File read(long appId, String url) {
        var path = getPath(appId, url);
        return new File(
                Files.readAllBytes(path),
                Files.probeContentType(path)
        );
    }

    private Path getPath(long appId, String url) {
        if (!url.startsWith("/uploads/"))
            throw new IllegalArgumentException("Invalid URL: " +  url);
        return uploadDir.resolve(Long.toString(appId)).resolve(url.substring("/uploads/".length()));
    }

    private String convertFileName(String fileName) {
        var lastIdx = fileName.lastIndexOf('.');
        var ext = fileName.substring(lastIdx);
        return UUID.randomUUID() + ext;
    }

    public boolean exists(long appId, String url) {
        if (url.startsWith("/uploads/"))
            return Files.exists(getPath(appId, url));
        else
            return false;
    }

    public String getMimeType(long appId, String url) {
        var fileName = getPath(appId, url);
        var path = uploadDir.resolve(fileName);
        try {
            return Files.probeContentType(path);
        } catch (Exception e) {
            return "application/octet-stream"; // Fallback MIME type
        }
    }

}
