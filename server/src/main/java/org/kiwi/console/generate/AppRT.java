package org.kiwi.console.generate;

import org.kiwi.console.kiwi.*;
import org.kiwi.console.kiwi.Module;
import org.kiwi.console.util.Utils;

import java.util.*;

public class AppRT {

    public static AppRT from(App app, AppConfig appConfig, ModuleTypeClient modTypeClient, AppClient appClient) {
        var modTypeIdSet = new HashSet<String>();
        app.getModules().forEach(mod -> modTypeIdSet.add(mod.getTypeId()));
        modTypeIdSet.add(appConfig.getFrontendModTypeId());
        modTypeIdSet.add(appConfig.getBackendModTypeId());
        var modTypeIds = new ArrayList<>(modTypeIdSet);
        var modTypes = modTypeClient.multiGet(new MultiGetRequest(modTypeIds));
        var modTypeMap = Utils.toMap(modTypeIds, modTypes);
        var mods = new ArrayList<ModuleRT>();
        var dependencyIds = new HashMap<String, List<String>>();
        for (Module module : app.getModules()) {
            mods.add(new ModuleRT(
                    module.getId(),
                    module.getName(),
                    module.getDescription(),
                    module.getProjectName(),
                    module.getTech(),
                    Objects.requireNonNull(modTypeMap.get(module.getTypeId()))
            ));
            dependencyIds.put(module.getId(), module.getDependencyIds());
        }
        var modMap =  Utils.toMap(mods, ModuleRT::id);
        for (ModuleRT mod : mods) {
            var depIds = Objects.requireNonNull(dependencyIds.get(mod.id()));
            for (String depId : depIds) {
                mod.addDependency(modMap.get(Objects.requireNonNull(depId)));
            }
        }
        var tech2modType = Map.of(
                Tech.KIWI, modTypeMap.get(appConfig.getBackendModTypeId()),
                Tech.WEB, modTypeMap.get(appConfig.getFrontendModTypeId())
        );
        return new AppRT(
                app.getId(),
                app.getName(),
                app.getOwnerId(),
                app.getMemberIds(),
                app.getKiwiAppId(),
                app.getPlanConfigId(),
                tech2modType,
                mods,
                appClient
        );
    }

    private final String id;
    private String name;
    private final String ownerId;
    private final long kiwiAppId;
    private String planConfigId;
    private final List<String> memberIds;
    private final List<ModuleRT> modules;
    private final Map<String, ModuleRT> moduleMap;
    private final AppClient appClient;
    private final Map<Tech, ModuleType> moduleTypeMap;

    public AppRT(String id, String name, String ownerId, List<String> memberIds, long kiwiAppId,
                 String planConfigId, Map<Tech, ModuleType> moduleTypeMap,
                 List<ModuleRT> modules, AppClient appClient) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.memberIds = new ArrayList<>(memberIds);
        this.kiwiAppId = kiwiAppId;
        this.planConfigId = planConfigId;
        this.moduleTypeMap = new HashMap<>(moduleTypeMap);
        this.modules = modules;
        moduleMap = Utils.toMap(modules, ModuleRT::id);
        this.appClient = appClient;
    }

    public ModuleRT getModule(String id) {
        return moduleMap.get(id);
    }

    public ModuleRT getModuleByName(String name) {
        return Utils.findRequired(modules, mod -> mod.name().equals(name),
                () -> new RuntimeException("Cannot find module with name '" + name + "'"));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getKiwiAppId() {
        return kiwiAppId;
    }

    public List<ModuleRT> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public void setName(String name) {
        this.name = name;
        appClient.updateName(new UpdateNameRequest(id, name));
    }

    public ModuleRT addModule(String name, String description, Tech tech, List<ModuleRT> dependencies) {
        var modType = Objects.requireNonNull(moduleTypeMap.get(tech), () -> "Cannot find module type for tech: " + tech);
        var projName = kiwiAppId + "-" + name;
        var mod = new Module(
                null,
                name,
                projName,
                tech,
                description,
                modType.getId(),
                Utils.map(dependencies, ModuleRT::id)
        );
        var app = build();
        app.addModule(mod);
        appClient.save(app);
        var updatedApp = appClient.get(id);
        var newMod = updatedApp.getModules().getLast();
        var modRT = new ModuleRT(
                newMod.getId(),
                name,
                description,
                projName,
                tech,
                modType
        );
        modRT.setDependencies(dependencies);
        modules.add(modRT);
        moduleMap.put(modRT.id(), modRT);
        return modRT;
    }

    private App build() {
        return new App(
                id,
                name,
                ownerId,
                kiwiAppId,
                memberIds,
                planConfigId,
                Utils.map(modules, ModuleRT::build)
        );
    }

    public void onChange() {
        appClient.save(build());
    }

}
