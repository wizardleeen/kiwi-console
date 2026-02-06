package org.kiwi.console.kiwi;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;

import java.util.List;

public interface ModuleTypeClient {

    @RequestLine("POST /module-types")
    @Headers("Content-Type: application/json")
    String save(ModuleType moduleType);

    @RequestLine("GET /module-types/{id}")
    ModuleType get(@Param("id") String id);

    @RequestLine("GET /module-types?id={ids}")
    SearchResult<ModuleType> multiGet(@Param("ids") List<String> ids);

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
