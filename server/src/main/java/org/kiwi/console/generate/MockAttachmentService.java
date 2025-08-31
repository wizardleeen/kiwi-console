package org.kiwi.console.generate;

import org.kiwi.console.file.File;

import java.io.InputStream;

public class MockAttachmentService implements AttachmentService {
    @Override
    public String upload(String fileName, InputStream input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String uploadConsoleLog(String appId, String fileName, InputStream input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public File read(String fileName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMimeType(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String url) {
        throw new UnsupportedOperationException();
    }
}
