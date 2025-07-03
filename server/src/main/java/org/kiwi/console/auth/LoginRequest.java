package org.kiwi.console.auth;

public record LoginRequest(
        String userName,
        String password
) {
}
