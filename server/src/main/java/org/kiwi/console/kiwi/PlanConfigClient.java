package org.kiwi.console.kiwi;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.Utils;

public interface PlanConfigClient {

    default String save(PlanConfig planConfig) {
        if (planConfig.getId() != null) {
            update(planConfig.getId(), planConfig);
            return planConfig.getId();
        } else {
            return create(planConfig);
        }
    }

    @RequestLine("POST /plan-configs")
    @Headers("Content-Type: application/json")
    String create(PlanConfig planConfig);

    @RequestLine("PATCH /plan-configs/{id}")
    @Headers("Content-Type: application/json")
    void update(@Param("id") String id, PlanConfig planConfig);

    @RequestLine("GET /plan-configs/{id}")
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
