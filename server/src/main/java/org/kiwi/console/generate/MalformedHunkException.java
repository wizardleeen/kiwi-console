package org.kiwi.console.generate;

public class MalformedHunkException extends RuntimeException {

    public MalformedHunkException(String text, int position, String reason) {
        super("Malformed hunk: " + text + ", position: " + (position + 1) + ", reason: " + reason);
    }

    public MalformedHunkException(String text, int position) {
        super("Malformed hunk: " + text + ", position: " + (position + 1));
    }
}
