package org.kiwi.console.kiwi;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;

import java.util.List;

public interface AppClient {

    @RequestLine("POST /api/application/_search")
    @Headers("Content-Type: application/json")
    SearchResult<App> search(AppSearchRequest request);

    @RequestLine("GET /api/application/{id}")
    App get(@Param("id") String id);

    @RequestLine("POST /api/application")
    @Headers("Content-Type: application/json")
    String save(App app);

    @RequestLine("POST /api/application/update-name")
    @Headers("Content-Type: application/json")
    void updateName(UpdateNameRequest request);

    @RequestLine("POST /api/application-service/delete-app")
    @Headers("Content-Type: application/json")
    void delete(DeleteAppRequest request);

    @RequestLine(("POST /api/application-service/create-app"))
    @Headers("Content-Type: application/json")
    String create(CreateAppRequest request);

    static void main(String[] args) {
        var client = Utils.createKiwiFeignClient(
                "http://localhost:8080",
                AppClient.class,
                Constants.CHAT_APP_ID
        );
        var id = "0192fbdab90700";
        for (int i = 0; i < 10; i++) {
            var id1 = client.save(new App(id, "test" + i, Constants.USER_ID, 0, List.of(), "", List.of()));
            System.out.println("Saved successfully. ID: " + id1);

        }
    }

}
