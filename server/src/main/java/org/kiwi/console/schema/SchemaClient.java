package org.kiwi.console.schema;

import feign.Param;
import feign.RequestLine;
import org.kiwi.console.schema.dto.SchemaResponse;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.Utils;

public interface SchemaClient {

    @RequestLine("GET /internal-api/schema/{appId}")
    SchemaResponse get(@Param("appId") long appId);


    static void main(String[] args) {
        var client = Utils.createFeignClient(Constants.KIWI_HOST, SchemaClient.class);
        var schema = client.get(1000179286);
        System.out.println(Utils.toPrettyJSONString(schema));
    }

}
