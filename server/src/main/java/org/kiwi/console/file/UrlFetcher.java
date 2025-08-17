package org.kiwi.console.file;

import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UrlFetcher {

    public static final long DEFAULT_MAX_SIZE_BYTES = 20 * 1024 * 1024; // 20MB
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    private final String defaultHost;
    private final long maxSizeBytes;
    private final HttpClient httpClient;

    public UrlFetcher(String defaultHost, long maxSizeBytes) {
        if (defaultHost == null || defaultHost.isBlank()) {
            throw new IllegalArgumentException("Default host cannot be null or empty.");
        }
        // Ensure host doesn't end with a slash to avoid double slashes.
        this.defaultHost = defaultHost.endsWith("/") ? defaultHost.substring(0, defaultHost.length() - 1) : defaultHost;
        this.maxSizeBytes = maxSizeBytes;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .build();
    }

    public UrlFetcher(String defaultHost) {
        this(defaultHost, DEFAULT_MAX_SIZE_BYTES);
    }

    @SneakyThrows
    public UrlResource fetch(String urlOrPath) {
        String finalUrlString = normalizeUrl(urlOrPath);
        URI uri;
        try {
            uri = URI.create(finalUrlString);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("The final constructed URL is invalid: " + finalUrlString);
        }

        var request = HttpRequest.newBuilder()
                .uri(uri)
                .GET() // Default, but explicit is good
                .timeout(Duration.ofSeconds(15)) // Per-request timeout
                .build();

        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // Check for non-successful status codes
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException(
                        "Failed to fetch URL: " + finalUrlString + ". Server responded with code: " + response.statusCode()
                );
            }

            // Check Content-Length header as a pre-emptive optimization
            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (contentLength > maxSizeBytes) {
                throw new RuntimeException(
                        "Resource size (" + contentLength + " bytes) exceeds limit of " + maxSizeBytes + " bytes (checked via header)."
                );
            }

            // Get MimeType from headers
            String mimeType = response.headers().firstValue("Content-Type").orElse("application/octet-stream").trim();
            var semiColonIdx = mimeType.indexOf(';');
            if (semiColonIdx != -1)
                mimeType = mimeType.substring(0, semiColonIdx).trim();
            // Read from the input stream with our size-limiting logic
            byte[] content;
            try (InputStream bodyStream = response.body()) {
                content = readLimitedBytes(bodyStream);
            }

            // response.uri() gives the final URI after any redirects
            return new UrlResource(content, mimeType, response.uri().toString());

        } catch (IOException e) {
            throw new RuntimeException("A network error occurred while fetching " + finalUrlString, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve the interrupted status
            throw new RuntimeException("The fetch operation was interrupted for " + finalUrlString, e);
        }
    }

    /**
     * Reads bytes from an InputStream up to the configured maxSizeBytes limit.
     */
    private byte[] readLimitedBytes(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long totalBytesRead = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                if (totalBytesRead > maxSizeBytes) {
                    throw new RuntimeException(
                        "Resource size exceeds the limit of " + maxSizeBytes + " bytes during download."
                    );
                }
                byteOut.write(buffer, 0, bytesRead);
            }
            return byteOut.toByteArray();
        }
    }

    /**
     * Validates and normalizes the input string into a full URL.
     */
    private String normalizeUrl(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) {
            throw new RuntimeException("URL or path cannot be null or empty.");
        }

        String trimmed = urlOrPath.trim();

        if (trimmed.toLowerCase().startsWith("http://") || trimmed.toLowerCase().startsWith("https://")) {
            return trimmed;
        }

        if (trimmed.startsWith("/")) {
            return defaultHost + trimmed;
        }

        throw new RuntimeException(
            "Input '" + urlOrPath + "' is not a valid HTTP/HTTPS URL or an absolute path (starting with '/')."
        );
    }

    public static void main(String[] args) {
        var fetcher = new UrlFetcher("https://1000061024.metavm.test");
//        var url = "https://1000061024.metavm.test/uploads/8aa6bc28-58b5-4681-8327-9103f623e165.png";
//        var url = "https://media.planview.com/clarizen/wp-content/upload/2021/04/MyWork-Tasks-1024x443.png";
        var url = "https://res.cloudinary.com/monday-blogs/fl_lossy,f_auto,q_auto/wp-blog/2024/08/task-management-kanban-view.jpg";
        var r = fetcher.fetch(url);
        System.out.println("mime type:" + r.mimeType());
        System.out.println("bytes: " + r.content().length);
    }

}