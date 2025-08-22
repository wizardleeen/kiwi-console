package org.kiwi.console.auth;

public record LoginResult(String token, UserDTO user) {
}
