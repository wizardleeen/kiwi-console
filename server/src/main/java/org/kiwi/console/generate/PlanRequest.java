package org.kiwi.console.generate;

import org.kiwi.console.file.File;

import java.util.List;

public record PlanRequest(
        Model model,
        String createTemplate,
        String updateTemplate,
        String requirement,
        List<File> attachments,
        List<ModuleInfo> modules,
        boolean first,
        AbortController abortController
) {
}
