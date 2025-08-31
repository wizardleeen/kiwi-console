package org.kiwi.console.generate;

import lombok.SneakyThrows;
import org.kiwi.console.StackTraceDeobfuscator;
import org.kiwi.console.file.File;
import org.kiwi.console.file.FileService;
import org.kiwi.console.kiwi.AppClient;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.Utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AttachmentServiceImpl implements AttachmentService {
    private final long chatAppId;
    private final FileService fileService;
    private final Path sourcemapRoot;
    private final AppClient appClient;

    public AttachmentServiceImpl(long chatAppId, FileService fileService, Path sourcemapRoot, AppClient appClient) {
        this.chatAppId = chatAppId;
        this.fileService = fileService;
        this.sourcemapRoot = sourcemapRoot;
        this.appClient = appClient;
    }

    @Override
    public String upload(String fileName, InputStream input) {
        return fileService.upload(chatAppId, fileName, input);
    }

    @SneakyThrows
    @Override
    public String uploadConsoleLog(String appId, String fileName, InputStream input) {
        var sourceMapPath = getSourceMapPath(appId);
        if (Files.isRegularFile(sourceMapPath)) {
            var sourceMap = Files.readString(sourceMapPath);
            var log = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            var deobfuscated = StackTraceDeobfuscator.deobfuscate(sourceMap, log);
            return upload(fileName, new ByteArrayInputStream(deobfuscated.getBytes(StandardCharsets.UTF_8)));
        } else
            return upload(fileName, input);
    }

    @SneakyThrows
    private Path getSourceMapPath(String appId) {
        var kiwiAppId = appClient.get(appId).getKiwiAppId();
        return sourcemapRoot.resolve(Long.toString(kiwiAppId)).resolve("index.js.map");
    }

    @Override
    public File read(String uri) {
        return fileService.read(chatAppId, uri);
    }

    @Override
    public String getMimeType(String url) {
        return fileService.getMimeType(chatAppId, url);
    }

    @Override
    public boolean exists(String url) {
        return fileService.exists(chatAppId, url);
    }

    public static void main(String[] args) {
        var service = new AttachmentServiceImpl(Constants.CHAT_APP_ID, new FileService(Path.of("/Users/leen/develop/uploads")),
                Path.of("/Users/leen/develop/page-works"),
                Utils.createFeignClient(
                        "http://localhost:8080",
                        AppClient.class
                ));
        var exists = service.exists("/uploads/ca29a734-7d0a-497e-ab7f-44c1ec8aec87.png");
        System.out.println(exists);
    }

}
