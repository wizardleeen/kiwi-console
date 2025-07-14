package org.kiwi.console.util;

public enum ErrorCode {
    BAD_REQUEST(101, "Bad request"),
    REQUEST_ERROR(102, "{}"),
    AUTHENTICATION_FAILED(401, "Authentication failed"),
    GEMINI_NOT_CONFIGURED(1801, "Gemini not configured"),
    CODE_GENERATION_FAILED(1802, "Code generation failed"),

    LOGIN_REQUIRED(1901, "Login required"),

    APP_CREATION_FAILURE(2001, "Application creation failed: {}"),
    APP_NOT_FOUND(2002,  "Application not found: {}"),
    FORBIDDEN(3002, "You do not have permission to perform this action"),

    // Generation
    TASK_NOT_RUNNING(4001, "Generation task not running"),
    TASK_CANCELLED(4002, "Generation task cancelled"),

    DEPLOY_FAILED(4003, "{}"),

    REQUIREMENT_REJECTED(4004, "{}"),

    GENERATION_ALREADY_RUNNING(4005, "Generation already running for this application")

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
