package org.kiwi.console.kiwi;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;

public interface ExchangeClient {

    @RequestLine("POST /api/exchange/_search")
    @Headers("Content-Type: application/json")
    SearchResult<Exchange> search(ExchangeSearchRequest request);

    @RequestLine("GET /api/exchange/{id}")
    Exchange get(@Param("id") String id);

    @RequestLine("POST /api/exchange")
    @Headers("Content-Type: application/json")
    String save(Exchange exchange);

    @RequestLine("DELETE /api/exchange/{id}")
    void delete(@Param("id") String id);

    @RequestLine("POST /api/exchange/cancel")
    @Headers("Content-Type: application/json")
    void cancel(ExchangeCancelRequest request);

    @RequestLine("POST /api/exchange/retry")
    @Headers("Content-Type: application/json")
    void retry(ExchangeRetryRequest request);

    static void main(String[] args) {
        var client = Utils.createKiwiFeignClient(
                "http://localhost:8080",
                ExchangeClient.class,
                Constants.CHAT_APP_ID
        );

        var id = "0190fcdab90700";
        var exch = client.get(id);


        for (int i = 0; i < 1000; i++) {
            exch.setStatus(i % 2 == 0 ? ExchangeStatus.SUCCESSFUL : ExchangeStatus.FAILED);
            client.save(exch);
        }

        System.out.println(id);


//        var r = client.search(new ExchangeSearchRequest(
//           null,
//           null,
//                null,
//                true,
//                1,
//                20
//        ));
//        System.out.println(r.total());
//        for (Exchange e : r.items()) {
//            System.out.println(e.getId() + " " + e.getStatus());
//        }
    }

}
