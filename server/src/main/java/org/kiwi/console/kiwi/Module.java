package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.kiwi.console.app.rest.ModuleDTO;

import java.util.List;

@Data
@AllArgsConstructor
public final class Module {
    private String id;
    private String name;
    private String projectName;
    private Tech tech;
    private String description;
    private String typeId;
    private List<String> dependencyIds;

    public static Module create(String name, String projectName, Tech type, String description, String configId) {
        return new Module(null, name, projectName, type, description, configId, List.of());
    }

    public ModuleDTO toDTO() {
        return new ModuleDTO(id, name, typeId, tech.name(), description, dependencyIds);
    }

}
