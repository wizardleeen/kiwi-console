package org.kiwi.console.kiwi;

import org.kiwi.console.util.Constants;
import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;

import java.util.UUID;

public class AppService implements AppClient {

    private final AppClient appClient;
    private final KiwiAppClient kiwiAppClient;
    private final UserClient userClient;

    public AppService(String url, long kiwiAppId, UserClient userClient) {
        this.userClient = userClient;
        this.appClient = Utils.createKiwiFeignClient(url, AppClient.class, kiwiAppId);
        this.kiwiAppClient = Utils.createFeignClient(url, KiwiAppClient.class);
    }

    @Override
    public SearchResult<App> search(AppSearchRequest request) {
        return appClient.search(request);
    }

    @Override
    public App get(String id) {
        return appClient.get(id);
    }

    @Override
    public String save(App app) {
        long kiwiAppId = app.getKiwiAppId();
        var user = userClient.get(app.getOwnerId());
        var kiwiUserId = user.getKiwiUserId();
        if (app.getKiwiAppId() == -1)
            kiwiAppId = kiwiAppClient.save(new SystemApp(null, app.getName(), kiwiUserId));
        else
            kiwiAppClient.save(new SystemApp(kiwiAppId, app.getName(), kiwiUserId));
        return appClient.save(new App(
                app.getId(),
                app.getName(),
                app.getOwnerId(),
                kiwiAppId,
                app.getMemberIds(),
                app.getPlanConfigId(),
                app.getModules()
        ));
    }

    @Override
    public void updateName(String id, UpdateNameRequest request) {
        appClient.updateName(id, request);
    }

    @Override
    public void delete(DeleteAppRequest request) {
        var r = appClient.get(request.appId());
        kiwiAppClient.delete(r.getKiwiAppId());
        appClient.delete(request);
    }

    @Override
    public String create(CreateAppRequest request) {
        var user = userClient.get(request.ownerId());
        var kiwiAppId = kiwiAppClient.save(new SystemApp(null, UUID.randomUUID().toString(), user.getKiwiUserId()));
        kiwiAppClient.updateName(new UpdateAppNameRequest(kiwiAppId, Long.toString(kiwiAppId)));
        return appClient.create(new CreateAppRequest(request.name(), kiwiAppId, request.ownerId()));
    }

    public static void main(String[] args) {
        var appService = new AppService(
                "http://localhost:8080",
                Constants.CHAT_APP_ID,
                Utils.createKiwiFeignClient(
                        "http://localhost:8080",
                        UserClient.class,
                        Constants.CHAT_APP_ID
                )
        );

        var app = appService.get("01e09ff3b90700");
        System.out.println(Utils.toPrettyJSONString(app));
    }

}
