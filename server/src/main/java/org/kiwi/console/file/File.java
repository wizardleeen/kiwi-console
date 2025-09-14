package org.kiwi.console.file;

public record File(String name, byte[] bytes, String mimeType) {

    public File(byte[] bytes, String mimeType) {
        this("unnamed", bytes, mimeType);
    }
}
