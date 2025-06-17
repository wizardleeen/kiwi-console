package org.kiwi.console.util;

public enum ErrorCode {
    VERIFICATION_FAILED(401, "Verification failed"),
    GEMINI_NOT_CONFIGURED(1801, "Gemini not configured"),
    CODE_GENERATION_FAILED(1802, "Code generation failed"),

    LOGIN_REQUIRED(1901, "Login required"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
