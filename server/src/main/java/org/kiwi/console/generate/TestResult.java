package org.kiwi.console.generate;

import org.kiwi.console.file.File;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record TestResult(
        boolean accepted,
        boolean aborted,
        @Nullable String abortReason,
        @Nullable String bugReport,
        @Nullable byte[] screenshot,
        @Nullable String dom,
        @Nullable String consoleLogs,
        String module) {

    public static final TestResult ACCEPTED = new TestResult(true, false, null, null, null, null, null, null);

    public static TestResult abort(String reason) {
        return new TestResult(false, true, reason, null, null, null, null, null);
    }

    public static TestResult reject(String bugReport, byte[] screenshot, String dom, String consoleLogs, String module) {
        return new TestResult(false, false, null, bugReport, screenshot, dom, consoleLogs, module);
    }

    public boolean rejected() {
        return !accepted && !aborted;
    }

    public String getFixRequirement() {
        return "Fix the following issue\n"
                + "**Module: ** " + Objects.requireNonNull(module) + "\n"
                + "**Description: **" + Objects.requireNonNull(bugReport, "bugReport is null");
    }

    public List<File> getAttachments() {
        var attachments = new ArrayList<File>();
        if (screenshot != null)
            attachments.add(new File(screenshot, "image/png"));
        if (dom != null)
            attachments.add(new File(dom.getBytes(), "text/html"));
        if (consoleLogs != null)
            attachments.add(new File(consoleLogs.getBytes(), "text/plain"));
        return attachments;
    }

}
