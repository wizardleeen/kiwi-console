package org.kiwi.console.kiwi;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.Utils;

public interface AppConfigClient {

    @RequestLine("POST /app-configs")
    @Headers("Content-Type: application/json")
    String save(AppConfig appConfig);

    @RequestLine("GET /app-configs/{id}")
    AppConfig get(@Param("id") String id);

    static void main(String[] args) {
        var client = Utils.createKiwiFeignClient(
                "http://localhost:8080",
                AppConfigClient.class,
                Constants.CHAT_APP_ID
        );
        var config = client.get("01b884f3b90700");
        System.out.println(Utils.toPrettyJSONString(config));

    }

}
