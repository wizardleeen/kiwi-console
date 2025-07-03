package org.kiwi.console.kiwi;

import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

public class MockExChangeClient implements ExchangeClient {

    private final Map<String, Exchange> exchanges = new LinkedHashMap<>();

    @Override
    public SearchResult<Exchange> search(ExchangeSearchRequest request) {
        var items = exchanges.values().stream()
                .filter(exch -> exch.getAppId().equals(request.appId()))
                .toList();
        return new SearchResult<>(items, items.size());
    }

    @Override
    public Exchange get(String id) {
        return copy(exchanges.get(id));
    }

    @Override
    public String save(Exchange exchange) {
        exchange = copy(exchange);
        if (exchange.getId() == null) {
            // random ID
            var id = String.valueOf(System.currentTimeMillis());
            exchange.setId(id);
        }
        exchanges.put(exchange.getId(), exchange);
        return exchange.getId();
    }

    private Exchange copy(Exchange exchange) {
        return new Exchange(
                exchange.getId(),
                exchange.getAppId(),
                exchange.getUserId(),
                exchange.getPrompt(),
                exchange.getStatus(),
                Utils.map(exchange.getStages(), this::copyStage),
                exchange.getProductURL(),
                exchange.getManagementURL(),
                exchange.getErrorMessage(),
                exchange.isFirst(),
                exchange.isSkipPageGeneration()
        );
    }

    private Stage copyStage(Stage stage) {
        return new Stage(
                stage.getId(),
                stage.getType(),
                stage.getStatus(),
                Utils.map(stage.getAttempts(), this::copyAttempt)
        );
    }

    private Attempt copyAttempt(Attempt attempt) {
        return new Attempt(
                attempt.getId(),
                attempt.getStatus(),
                attempt.getErrorMessage()
        );
    }

    @Override
    public void delete(String id) {
        exchanges.remove(id);
    }

    @Override
    public void cancel(ExchangeCancelRequest request) {
        var exch = exchanges.get(request.exchangeId());
        exch.setStatus(ExchangeStatus.CANCELLED);
    }

    @Override
    public void retry(ExchangeRetryRequest request) {
        var exch = exchanges.get(request.exchangeId());
        if (exch.getStatus() != ExchangeStatus.FAILED)
            throw new IllegalStateException("Exchange is not in FAILED state");
        exch.setStatus(ExchangeStatus.PLANNING);
    }

    public Exchange getFirst() {
        return copy(exchanges.values().stream().findFirst().orElseThrow());
    }

    public Exchange getLast() {
        if (exchanges.isEmpty())
            throw new NoSuchElementException("No exchanges found");
        Exchange last = null;
        for (Exchange value : exchanges.values()) {
            last = value;
        }
        return Objects.requireNonNull(last);
    }

}
