package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppConfig {
    private String id;
    private String name;
    private String planConfigId;
    private String backendModTypeId;
    private String frontendModTypeId;
}
