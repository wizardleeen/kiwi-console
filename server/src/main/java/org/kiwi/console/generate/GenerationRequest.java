package org.kiwi.console.generate;

import lombok.Data;
import org.kiwi.console.file.File;

import java.util.List;

@Data
public class GenerationRequest {
    private final Model model;
    private final long appId;
    private final String projectName;
    private final String appName;
    private final String createTemplate;
    private final String updateTemplate;
    private final String fixTemplate;
    private final String requirement;
    private final String suggestion;
    private final List<File> attachments;
    private final AbortController abortController;
    private final boolean deploySource;
    private final boolean outputThinking;
    private final boolean noBackup;
    private final List<SourceFile> apiFiles;
    private final CodeAgentListener listener;
}
