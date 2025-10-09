package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanConfig {
    private String id;
    private String name;
    private String model;
    private String createPromptTemplate;
    private String updatePromptTemplate;
}
