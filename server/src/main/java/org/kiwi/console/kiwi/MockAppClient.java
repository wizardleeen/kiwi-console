package org.kiwi.console.kiwi;

import org.kiwi.console.util.SearchResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class MockAppClient implements AppClient {

    private final Map<String, App> apps = new LinkedHashMap<>();
    private final Random random = new Random();
    private final UserClient userClient;

    public MockAppClient(UserClient userClient) {
        this.userClient = userClient;
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
        if (app.getId() == null) {
            app.setGenConfigId(userClient.get(app.getOwnerId()).getGenConfigId());
            app.setId(Long.toString(System.currentTimeMillis()));
            app.setSystemAppId(random.nextInt(10000));
        }
        apps.put(app.getId(), app);
        return app.getId();
    }

    private App copy(App app) {
        return new App(app.getId(), app.getName(), app.getOwnerId(),
                app.getSystemAppId(), app.getMemberIds(), app.getGenConfigId());
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
}
