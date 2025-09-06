package org.kiwi.console.browser;

import javax.annotation.Nullable;

public record ExecuteResult(
        boolean successful,
        @Nullable String errorMessage
) {

    private static final ExecuteResult success = new ExecuteResult(true, null);

    public static ExecuteResult failed(String errorMessage) {
        return new ExecuteResult(false, errorMessage);
    }

    public static ExecuteResult success() {
        return success;
    }

}
