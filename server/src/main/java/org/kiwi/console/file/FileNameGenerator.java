package org.kiwi.console.file;

import java.util.UUID;
import java.util.regex.Pattern;

public class FileNameGenerator {

    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9-]");
    private static final Pattern MULTIPLE_HYPHENS_PATTERN = Pattern.compile("-{2,}");

    public static String generateUniqueFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Original filename cannot be null or empty.");
        }

        String cleanFileName = originalFileName.substring(
            Math.max(originalFileName.lastIndexOf('/'), originalFileName.lastIndexOf('\\')) + 1
        );

        String baseName;
        String extension;
        int lastDotIndex = cleanFileName.lastIndexOf('.');

        if (lastDotIndex == -1) {
            baseName = cleanFileName;
            extension = "";
        } else {
            baseName = cleanFileName.substring(0, lastDotIndex);
            extension = cleanFileName.substring(lastDotIndex + 1);
        }

        String sanitizedBaseName = baseName.toLowerCase()
                .replaceAll("\\s+", "-"); // Replace one or more spaces with a hyphen

        sanitizedBaseName = NON_ALPHANUMERIC_PATTERN.matcher(sanitizedBaseName).replaceAll("");
        sanitizedBaseName = MULTIPLE_HYPHENS_PATTERN.matcher(sanitizedBaseName).replaceAll("-");
        
        sanitizedBaseName = sanitizedBaseName.replaceAll("^-|-$", "");

        String uniqueId = UUID.randomUUID().toString().replace("-", "");

        if (extension.isEmpty()) {
            if (sanitizedBaseName.isEmpty()) {
                return uniqueId;
            }
            return String.format("%s-%s", sanitizedBaseName, uniqueId);
        } else {
            return String.format("%s-%s.%s", sanitizedBaseName, uniqueId, extension.toLowerCase());
        }
    }

    public static void main(String[] args) {
        System.out.println("--- Standard Cases ---");
        System.out.println("My Resume!.pdf     -> " + generateUniqueFileName("My Resume!.pdf"));
        System.out.println("image (1).JPEG     -> " + generateUniqueFileName("image (1).JPEG"));
        
        System.out.println("\n--- Edge Cases ---");
        System.out.println("no-extension-file  -> " + generateUniqueFileName("no-extension-file"));
        System.out.println(".profile           -> " + generateUniqueFileName(".profile")); // Dotfile
        System.out.println("file.with.dots.txt -> " + generateUniqueFileName("file.with.dots.txt"));
        
        System.out.println("\n--- Path Stripping Cases ---");
        System.out.println("C:\\Users\\file.zip -> " + generateUniqueFileName("C:\\Users\\file.zip"));
        System.out.println("/var/www/img.png   -> " + generateUniqueFileName("/var/www/img.png"));
        
        System.out.println("\n--- Sanitization Cases ---");
        System.out.println("  leading space.txt  -> " + generateUniqueFileName("  leading space.txt"));
        System.out.println("file___with--hyphens.doc -> " + generateUniqueFileName("file___with--hyphens.doc"));
        System.out.println("!@#$%.gif          -> " + generateUniqueFileName("!@#$%.gif"));
    }
}