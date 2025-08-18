package org.kiwi.console.kiwi;

import org.kiwi.console.util.Constants;
import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;

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
                app.getGenConfigId()
        ));
    }

    @Override
    public void updateName(UpdateNameRequest request) {
        var app = get(request.applicationId());
        var user = userClient.get(app.getOwnerId());
        kiwiAppClient.save(new SystemApp(app.getKiwiAppId(), request.name(), user.getKiwiUserId()));
        appClient.updateName(request);
    }

    @Override
    public void delete(DeleteAppRequest request) {
        var r = appClient.get(request.appId());
        kiwiAppClient.delete(r.getKiwiAppId());
        appClient.delete(request);
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

        var id = appService.save(App.create("test2", Constants.USER_ID));
        System.out.println("Application saved with ID: " + id);

//        appService.updateName(new UpdateNameRequest("01a49cdab90700", "Test Updated"));


//        appService.delete(new DeleteAppRequest("01a49cdab90700"));

        var searchResult = appService.search(new AppSearchRequest(
                null,
                "0182f5d7b90700",
                1,
                20,
                null
        ));
        System.out.println("Search successful, found " + searchResult.total() + " applications.");
        for (App item : searchResult.items()) {
            System.out.println("Application ID: " + item.getId() + ", Name: " + item.getName() +
                    ", Owner ID: " + item.getOwnerId() + ", System App ID: " + item.getKiwiAppId());
        }

//        var r = appService.get("01a49cdab90700");
//        System.out.println("Application found: " + r.getName());

    }

}
