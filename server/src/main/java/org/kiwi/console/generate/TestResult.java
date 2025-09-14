package org.kiwi.console.generate;

import javax.annotation.Nullable;

public record TestResult(
        boolean accepted,
        boolean aborted,
        @Nullable String abortReason,
        @Nullable String bugReport,
        @Nullable byte[] screenshot,
        @Nullable String dom,
        @Nullable String consoleLogs
) {

    public static final TestResult ACCEPTED = new TestResult(true, false, null, null, null, null, null);

    public static TestResult abort(String reason) {
        return new TestResult(false, true, reason, null, null, null, null);
    }

    public static TestResult reject(String bugReport, byte[] screenshot, String dom, String consoleLogs) {
        return new TestResult(false, false, null, bugReport, screenshot, dom, consoleLogs);
    }

}
