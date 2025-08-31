package org.kiwi.console.generate;

import org.kiwi.console.file.File;

import java.io.InputStream;

public interface AttachmentService {
    String upload(String fileName, InputStream input);

    String uploadConsoleLog(String appId, String fileName, InputStream input);

    File read(String uri);

    String getMimeType(String url);

    boolean exists(String url);

}
