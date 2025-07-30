package org.kiwi.console.generate;

import org.kiwi.console.file.File;
import org.kiwi.console.file.FileService;
import org.kiwi.console.util.Constants;

import java.io.InputStream;
import java.nio.file.Path;

public class AttachmentServiceImpl implements AttachmentService {
    private final long chatAppId;
    private final FileService fileService;

    public AttachmentServiceImpl(long chatAppId, FileService fileService) {
        this.chatAppId = chatAppId;
        this.fileService = fileService;
    }

    @Override
    public String upload(String fileName, InputStream input) {
        return fileService.upload(chatAppId, fileName, input);
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
        var service = new AttachmentServiceImpl(Constants.CHAT_APP_ID, new FileService(Path.of("/Users/leen/develop/uploads")));
        var exists = service.exists("/uploads/ca29a734-7d0a-497e-ab7f-44c1ec8aec87.png");
        System.out.println(exists);
    }

}
