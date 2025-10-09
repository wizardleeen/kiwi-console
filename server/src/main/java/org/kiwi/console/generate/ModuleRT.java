package org.kiwi.console.generate;

import org.kiwi.console.kiwi.Module;
import org.kiwi.console.kiwi.ModuleType;
import org.kiwi.console.kiwi.Tech;
import org.kiwi.console.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleRT {
    private final String id;
    private final String name;
    private final String description;
    private final String projectName;
    private final Tech tech;
    private final ModuleType type;
    private final List<ModuleRT> dependencies = new ArrayList<>();

    public ModuleRT(
            String id,
            String name,
            String description,
            String projectName,
            Tech tech,
            ModuleType type
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.projectName = projectName;
        this.tech = tech;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String projectName() {
        return projectName;
    }

    public Tech tech() {
        return tech;
    }

    public ModuleType type() {
        return type;
    }

    public String description() {
        return description;
    }

    public List<ModuleRT> dependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public void addDependency(ModuleRT dependency) {
        dependencies.add(dependency);
    }

    public void setDependencies(List<ModuleRT> dependencies) {
        this.dependencies.clear();
        this.dependencies.addAll(dependencies);
    }

    public Module build() {
        return new Module(
                id,
                name,
                projectName,
                tech,
                description,
                type.getId(),
                Utils.map(dependencies, ModuleRT::id)
        );
    }
}
