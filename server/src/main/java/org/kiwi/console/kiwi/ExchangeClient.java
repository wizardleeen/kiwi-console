package org.kiwi.console.kiwi;

import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;

import java.util.List;

public interface ExchangeClient {

    @RequestLine("GET /exchanges")
    SearchResult<Exchange> search(@QueryMap ExchangeSearchRequest request);

    @RequestLine("GET /exchanges/{id}")
    Exchange get(@Param("id") String id);

    @RequestLine("POST /exchanges")
    @Headers("Content-Type: application/json")
    String create(Exchange exchange);

    @RequestLine("PATCH /exchanges/{id}")
    @Headers("Content-Type: application/json")
    void update(@Param("id") String id, Exchange exchange);

    default String save(Exchange exchange) {
        if (exchange.getId() != null) {
            update(exchange.getId(), exchange);
            return exchange.getId();
        } else {
            return create(exchange);
        }
    }

    @RequestLine("DELETE /exchanges/{id}")
    void delete(@Param("id") String id);

    @RequestLine("POST /exchanges/{id}/cancel")
    @Headers("Content-Type: application/json")
    void cancel(@Param("id") String id);

    @RequestLine("POST /exchanges/{id}/retry")
    @Headers("Content-Type: application/json")
    void retry(@Param("id") String id);

    @RequestLine("POST /exchange-service/revert-exchange")
    @Headers("Content-Type: application/json")
    void revert(ExchangeIdRequest request);

    @RequestLine("POST /exchange-service/fail-expired-exchanges")
    @Headers("Content-Type: application/json")
    List<String> failExpiredExchanges();

    @RequestLine("POST /exchanges/{id}/send-heart-beat")
    @Headers("Content-Type: application/json")
    void sendHeartBeat(@Param("id") String id);

    @RequestLine("POST /exchange-service/is-generating")
    @Headers("Content-Type: application/json")
    boolean isGenerating(IsGeneratingRequest request);

    static void main(String[] args) {
        var client = Utils.createKiwiFeignClient(
                "http://localhost:8080",
                ExchangeClient.class,
                Constants.CHAT_APP_ID
        );
//
//        var id = "0190fcdab90700";
//        var exch = client.get(id);
//
//
//        for (int i = 0; i < 1000; i++) {
//            exch.setStatus(i % 2 == 0 ? ExchangeStatus.SUCCESSFUL : ExchangeStatus.FAILED);
//            client.save(exch);
//        }
//
//        System.out.println(id);

//        client.failExpiredExchanges();

        System.out.println(client.isGenerating(new IsGeneratingRequest("01fc87e1b90700")));

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
