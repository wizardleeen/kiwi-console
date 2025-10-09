package org.kiwi.console.kiwi;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.Utils;

import java.util.List;

public interface ModuleTypeClient {

    @RequestLine("POST /api/module-type")
    @Headers("Content-Type: application/json")
    String save(ModuleType moduleType);

    @RequestLine("GET /api/module-type/{id}")
    ModuleType get(@Param("id") String id);

    @RequestLine("POST /api/module-type/_multi-get")
    @Headers("Content-Type: application/json")
    List<ModuleType> multiGet(MultiGetRequest request);

    static void main(String[] args) {
        var client = Utils.createKiwiFeignClient(
                "http://localhost:8080",
                ModuleTypeClient.class,
                Constants.CHAT_APP_ID
        );
        var modType = client.get("01be84f3b90700");
        System.out.println(Utils.toPrettyJSONString(modType));
    }

}
