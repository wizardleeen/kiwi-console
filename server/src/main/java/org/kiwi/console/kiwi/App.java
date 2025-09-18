package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.kiwi.console.app.rest.AppDTO;
import org.kiwi.console.util.Utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class App {
    private String id;
    private String name;
    private String ownerId;
    private long kiwiAppId;
    private List<String> memberIds;
    private String planConfigId;
    private List<Module> modules;

    public static App create(String name, String ownerId) {
        return create(name, ownerId, List.of());
    }

    public static App create(String name, String ownerId, List<Module> modules) {
        return new App(null, name, ownerId, -1, List.of(), "", modules);
    }

    public AppDTO toDTO() {
        return new AppDTO(id, name, ownerId, modules == null ? List.of() : Utils.map(modules, Module::toDTO));
    }

    public void addModule(Module module) {
        modules.add(module);
    }

    public Module getModuleByName(String name) {
        return Utils.findRequired(modules, mod -> mod.getName().equals(name),
                () -> new NoSuchElementException("No module named '" + name + "' in app '" + this.name + "'"));
    }

    public boolean hasDependencyCircle() {
        var visiting = new HashSet<Module>();
        var visited = new HashSet<Module>();
        var moduleMap = modules.stream().collect(Collectors.toMap(Module::getId, Function.identity()));
        for (Module module : modules) {
            if (hasDependencyCircle0(visiting, visited, moduleMap, module))
                return true;
        }
        return false;
    }

    private boolean hasDependencyCircle0(Set<Module> visiting, Set<Module> visited, Map<String, Module> moduleMap, Module current) {
        if (visited.contains(current))
            return false;
        if (!visiting.add(current))
            return true;
        for (String dependencyId : current.getDependencyIds()) {
            var dep = moduleMap.get(dependencyId);
            if (hasDependencyCircle0(visiting, visited, moduleMap, dep))
                return true;
        }
        visiting.remove(current);
        visited.add(current);
        return false;
    }


}
