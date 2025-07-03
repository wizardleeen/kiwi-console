package org.kiwi.console.util;

import javax.annotation.Nullable;

public record Result<T>(int code, @Nullable String message, T data) {
    public static <T> Result<T> success(T data) {
        return new Result<>(0, null, data);
    }

    public static Result<Void> voidSuccess() {
        return new Result<>(0, null, null);
    }

    public static <T> Result<T> failure(ErrorCode code, Object...args) {
        return new Result<>(code.getCode(), ResultUtil.formatMessage(code, args), null);
    }

    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccessful() {
        return code == 0;
    }
}
