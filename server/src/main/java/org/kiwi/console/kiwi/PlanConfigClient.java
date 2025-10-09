package org.kiwi.console.kiwi;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.Utils;

public interface PlanConfigClient {

    @RequestLine("POST /api/plan-config")
    @Headers("Content-Type: application/json")
    String save(PlanConfig planConfig);

    @RequestLine("GET /api/plan-config/{id}")
    PlanConfig get(@Param("id") String id);

    static void main(String[] args) {
        var client = Utils.createKiwiFeignClient(
                "http://localhost:8080",
                PlanConfigClient.class,
                Constants.CHAT_APP_ID
        );
        var config = client.get("01ba84f3b90700");
        System.out.println(Utils.toPrettyJSONString(config));
    }

}
