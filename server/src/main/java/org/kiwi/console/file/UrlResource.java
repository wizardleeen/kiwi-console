package org.kiwi.console.file;

public record UrlResource(byte[] content, String mimeType, String finalUrl) {}