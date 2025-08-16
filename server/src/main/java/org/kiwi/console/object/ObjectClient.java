package org.kiwi.console.object;

import feign.Headers;
import feign.RequestLine;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;

import java.util.List;
import java.util.Map;

public interface ObjectClient {

    @RequestLine("POST /internal-api/object/save")
    @Headers("Content-Type: application/json")
    String save(SaveRequest request);

    @RequestLine("POST /internal-api/object/get")
    @Headers("Content-Type: application/json")
    Map<String, Object> get(GetRequest request);

    @RequestLine("POST /internal-api/object/multi-get")
    @Headers("Content-Type: application/json")
    List<Map<String, Object>> multiGet(MultiGetRequest request);

    @RequestLine("POST /internal-api/object/search")
    @Headers("Content-Type: application/json")
    SearchResult<Object> search(SearchRequest request);

    @RequestLine("POST /internal-api/object/delete")
    @Headers("Content-Type: application/json")
    void delete(DeleteRequest request);

    @RequestLine("POST /internal-api/object/invoke")
    @Headers("Content-Type: application/json")
    Object invoke(InvokeRequest request);

    static void main(String[] args) {
        var client = Utils.createFeignClient(Constants.KIWI_HOST, ObjectClient.class);
        var appId = 1000178662;
        var id = "01a4c5ecb90700";
//        var id = client.save(
//                new SaveRequest(
//                        appId,
//                        Map.of(
//                                "type", "domain.Task",
//                                "fields", Map.of(
//                                    "title", "Demo",
//                                    "description","demo",
//                                    "completed", false
//                                )
//                        )
//                )
//        );
//        System.out.println(id);

//        client.delete(new DeleteRequest(appId, "0194c5ecb90700"));

//        var object = client.get(new GetRequest(appId, "0194c5ecb90700"));
//        System.out.println(Utils.toPrettyJSONString(object));

        client.invoke(new InvokeRequest(
                appId,
                Map.of("id", id),
                "markAsCompleted",
                Map.of()
        ));

        var r = client.search(new SearchRequest(
                appId,
                "domain.Task",
                Map.of("title", "demo"),
                null,
                0,
                0
        ));
        System.out.println(Utils.toPrettyJSONString(r));

        var r1 = client.multiGet(new MultiGetRequest(
                appId, List.of(id), true, true
        ));
        System.out.println(Utils.toPrettyJSONString(r1));

    }

}
