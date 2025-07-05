package org.kiwi.console.kiwi;

public record RegisterRequest(String userName, String password, String sysUserId) {

    public RegisterRequest(String userName, String password) {
        this(userName, password, null);
    }
}
