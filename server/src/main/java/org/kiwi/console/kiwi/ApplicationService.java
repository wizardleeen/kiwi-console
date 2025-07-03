package org.kiwi.console.kiwi;

import feign.RequestLine;
import org.kiwi.console.util.*;

public class ApplicationService implements ApplicationClient {

    private final ApplicationClient appClient;
    private final SystemAppClient systemAppClient;

    public ApplicationService(String url, long sysAppId, String token) {
        this.appClient = Utils.createKiwiFeignClient(url, ApplicationClient.class, sysAppId);
        this.systemAppClient = Utils.createFeignClient(
                url, SystemAppClient.class, rt -> {
                    rt.header("X-App-ID", "2");
                    rt.header("Cookie", "__token_2__=" + token);
                }
        );
    }

    @Override
    @RequestLine("POST /api/application/_search")
    public SearchResult<Application> search(ApplicationSearchRequest request) {
        return appClient.search(request);
    }

    @Override
    @RequestLine("GET /api/application/{id}")
    public Application get(String id) {
        return appClient.get(id);
    }

    @Override
    @RequestLine("POST /api/application")
    public String save(Application application) {
        long sysAppId = application.getSystemAppId();
        if (application.getSystemAppId() == -1) {
            var r = systemAppClient.save(new SystemApp(null, application.getName()));
            if (!r.isSuccessful())
                throw new BusinessException(ErrorCode.REQUEST_ERROR, r.message());
            sysAppId = r.data();
        } else {
            var r = systemAppClient.save(new SystemApp(sysAppId, application.getName()));
            if (!r.isSuccessful())
                throw new BusinessException(ErrorCode.REQUEST_ERROR, r.message());
        }
        return appClient.save(new Application(
                application.getId(),
                application.getName(),
                application.getOwnerId(),
                sysAppId,
                application.getMembersIds()
        ));
    }

    @Override
    public void updateName(UpdateNameRequest request) {
        var app = get(request.applicationId());
        systemAppClient.save(new SystemApp(app.getSystemAppId(), request.name()));
        appClient.updateName(request);
    }

    @Override
    public void delete(DeleteAppRequest request) {
        var r = appClient.get(request.appId());
        systemAppClient.delete(r.getSystemAppId());
        appClient.delete(request);
    }

    public static void main(String[] args) {
        var appService = new ApplicationService(
                "http://localhost:8080",
                Constants.TEST_SYS_APP_ID,
                Constants.TOKEN
        );

//        var id = appService.save(Application.create("test", Constants.USER_ID));
//        System.out.println("Application saved with ID: " + id);

        appService.updateName(new UpdateNameRequest("01a083d8b90700", "Test Updated"));


//        appService.delete(new DeleteAppRequest("019a99d7b90700"));

//        var searchResult = appService.search(new ApplicationSearchRequest(
//                "test",
//                userId,
//                1,
//                20,
//                id
//        ));
//        System.out.println("Search successful, found " + searchResult.total() + " applications.");

//        var r = appService.get("01cef7d6b90700");
//        if (r.isSuccessful()) {
//            System.out.println("Application found: " + r.data().getName());
//        } else {
//            System.err.println("Failed to get application: " + r.message());
//        }

    }

}
