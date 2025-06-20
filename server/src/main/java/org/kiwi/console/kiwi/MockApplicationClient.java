package org.kiwi.console.kiwi;

import org.kiwi.console.util.SearchResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class MockApplicationClient implements ApplicationClient {

    private final Map<String, Application> apps = new LinkedHashMap<>();
    private final Random random = new Random();

    @Override
    public SearchResult<Application> search(ApplicationSearchRequest request) {
        var matches = apps.values().stream().filter(app ->  match(app, request)).toList();
        return new SearchResult<>(
                matches.subList(
                        Math.min(matches.size(), (request.page() - 1) * request.pageSize()),
                        Math.min(matches.size(), request.page() * request.pageSize())
                ),
                matches.size()
        );
    }

    private boolean match(Application application, ApplicationSearchRequest searchRequest) {
        if (searchRequest.name() != null && !application.getName().contains(searchRequest.name()))
            return false;
        return searchRequest.ownerId() == null || application.getOwnerId().equals(searchRequest.ownerId());
    }

    @Override
    public Application get(String id) {
        var app = apps.get(id);
        if (app == null)
            throw new IllegalArgumentException("Application with ID " + id + " not found.");
        return copy(app);
    }

    @Override
    public String save(Application application) {
        application = copy(application);
        if (application.getId() == null) {
            application.setId(Long.toString(System.currentTimeMillis()));
            application.setSystemAppId(random.nextInt(10000));
        }
        apps.put(application.getId(), application);
        return application.getId();
    }

    private Application copy(Application application) {
        return new Application(application.getId(), application.getName(), application.getOwnerId(),
                application.getSystemAppId(), application.getMembersIds());
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
