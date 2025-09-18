package org.kiwi.console.kiwi;

import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;

import java.util.*;

public class MockAppClient implements AppClient {

    private final Map<String, App> apps = new LinkedHashMap<>();
    private final Random random = new Random();
    private final UserClient userClient;
    private final AppConfigClient appConfigClient;

    public MockAppClient(UserClient userClient, AppConfigClient appConfigClient) {
        this.userClient = userClient;
        this.appConfigClient = appConfigClient;
    }

    @Override
    public SearchResult<App> search(AppSearchRequest request) {
        var matches = apps.values().stream().filter(app ->  match(app, request)).toList();
        return new SearchResult<>(
                matches.subList(
                        Math.min(matches.size(), (request.page() - 1) * request.pageSize()),
                        Math.min(matches.size(), request.page() * request.pageSize())
                ),
                matches.size()
        );
    }

    private boolean match(App app, AppSearchRequest searchRequest) {
        if (searchRequest.name() != null && !app.getName().contains(searchRequest.name()))
            return false;
        return searchRequest.ownerId() == null || app.getOwnerId().equals(searchRequest.ownerId());
    }

    @Override
    public App get(String id) {
        var app = apps.get(id);
        if (app == null)
            throw new IllegalArgumentException("Application with ID " + id + " not found.");
        return copy(app);
    }

    @Override
    public String save(App app) {
        app = copy(app);
        if (app.getId() == null)
            app.setId(UUID.randomUUID().toString());
        for (Module module : app.getModules()) {
            if (module.getId() == null)
                module.setId(UUID.randomUUID().toString());
        }
        apps.put(app.getId(), app);
        return app.getId();
    }

    private App copy(App app) {
        return new App(app.getId(), app.getName(), app.getOwnerId(),
                app.getKiwiAppId(), app.getMemberIds(), app.getPlanConfigId(),
                Utils.map(app.getModules(), this::copyModule)
                );
    }

    private Module copyModule(Module module) {
        return new Module(module.getId(), module.getName(), module.getProjectName(), module.getTech(), module.getDescription(), module.getTypeId(), module.getDependencyIds());
    }

    @Override
    public void updateName(UpdateNameRequest request) {
        var app = apps.get(request.applicationId());
        if (app != null) {
            app.setName(request.name());
            apps.put(app.getId(), app);
        } else
            throw new IllegalArgumentException("Application with ID " + request.applicationId() + " not found.");
    }

    @Override
    public void delete(DeleteAppRequest request) {
        if (apps.remove(request.appId()) == null)
            throw new IllegalArgumentException("Application with ID " + request.appId() + " not found.");
    }

    @Override
    public String create(CreateAppRequest request) {
        var user = userClient.get(request.ownerId());
        var config = appConfigClient.get(user.getAppConfigId());
        var app = new App(
                UUID.randomUUID().toString(),
                request.name(),
                request.ownerId(),
                random.nextInt(10000),
                List.of(request.ownerId()),
                config.getPlanConfigId(),
                new ArrayList<>()
        );
        apps.put(app.getId(), app);
        var kiwiMod = new Module(
                UUID.randomUUID().toString(),
                "Kiwi",
                app.getKiwiAppId() + "",
                Tech.KIWI,
                "Kiwi",
                config.getBackendModTypeId(),
                List.of()
        );
        app.addModule(kiwiMod);
        var webMod = new Module(
                UUID.randomUUID().toString(),
                "Web",
                app.getKiwiAppId() + "",
                Tech.WEB,
                "Web",
                config.getFrontendModTypeId(),
                List.of(kiwiMod.getId())
        );
        app.addModule(webMod);
        return app.getId();
    }
}
