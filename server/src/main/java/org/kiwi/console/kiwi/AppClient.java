package org.kiwi.console.kiwi;

import feign.*;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;

import java.util.List;

public interface AppClient {

    @RequestLine("GET /applications")
    SearchResult<App> search(@QueryMap AppSearchRequest request);

    @RequestLine("GET /applications/{id}")
    App get(@Param("id") String id);

    @RequestLine("POST /applications")
    @Headers("Content-Type: application/json")
    String save(App app);

    @RequestLine("POST /applications/{id}/update-name")
    @Headers("Content-Type: application/json")
    void updateName(@Param("id") String id, UpdateNameRequest request);

    @RequestLine("POST /application-service/delete-app")
    @Headers("Content-Type: application/json")
    void delete(DeleteAppRequest request);

    @RequestLine(("POST /application-service/create-app"))
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
