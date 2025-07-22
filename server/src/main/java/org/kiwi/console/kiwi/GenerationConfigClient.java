package org.kiwi.console.kiwi;

import feign.Param;
import feign.RequestLine;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.Utils;

public interface GenerationConfigClient {

    @RequestLine("GET /api/generation-config/{id}")
    GenerationConfig get(@Param("id") String id);

    static void main(String[] args) {
        var client = Utils.createKiwiFeignClient(
                "http://localhost:8080",
                GenerationConfigClient.class,
                Constants.CHAT_APP_ID
        );
        var config = client.get("01fee4e1b90700");
        System.out.println(config.name());
    }

}
