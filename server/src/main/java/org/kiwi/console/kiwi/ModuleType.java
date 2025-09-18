package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.Nullable;

@Data
@AllArgsConstructor
public class ModuleType {
    private String id;
    private String name;
    private String description;
    private Tech tech;
    private String codeModel;
    private @Nullable String testModel;
    private String createPromptTemplate;
    private String updatePromptTemplate;
    private String fixPromptTemplate;
    private @Nullable String testPromptTemplate;
    private String templateRepository;
    private String templateBranch;
    private boolean testable;
    private boolean outputThinking;
}
