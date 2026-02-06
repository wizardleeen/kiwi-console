package org.kiwi.console.kiwi;

import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;

import java.util.*;

public class MockExchangeClient implements ExchangeClient {

    public static final long TIMEOUT = 60 * 1000;

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
        if (exchange.getId() == null)
            exchange.setId(UUID.randomUUID().toString());
        for (ExchangeTask task : exchange.getTasks()) {
            if (task.getId() == null)
                task.setId(UUID.randomUUID().toString());
            for (Attempt attempt : task.getAttempts()) {
                if (attempt.getId() == null)
                    attempt.setId(UUID.randomUUID().toString());
            }
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
                exchange.getAttachmentUrls(),
                exchange.getStatus(),
                exchange.getProductURL(),
                exchange.getManagementURL(),
                exchange.getSourceCodeURL(),
                exchange.getErrorMessage(),
                exchange.isFirst(),
                exchange.isSkipPageGeneration(),
                exchange.getLastHeartBeatAt(),
                exchange.getParentExchangeId(),
                exchange.getChainDepth(),
                exchange.isTestOnly(),
                Utils.map(exchange.getTasks(), this::copyTask)
        );
    }

    private ExchangeTask copyTask(ExchangeTask exchangeTask) {
        return new ExchangeTask(
                exchangeTask.getId(),
                exchangeTask.getModuleId(),
                exchangeTask.getModuleName(),
                exchangeTask.getType(),
                exchangeTask.getStatus(),
                exchangeTask.getErrorMessage(),
                Utils.map(exchangeTask.getAttempts(), this::copyAttempt)
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
    public void cancel(String id) {
        var exch = exchanges.get(id);
        exch.setStatus(ExchangeStatus.CANCELLED);
    }

    @Override
    public void retry(String id) {
        var exch = exchanges.get(id);
        if (exch.getStatus() != ExchangeStatus.FAILED)
            throw new IllegalStateException("Exchange is not in FAILED state");
        exch.setStatus(ExchangeStatus.PLANNING);
    }

    @Override
    public void revert(ExchangeIdRequest request) {
        var exch = exchanges.get(request.exchangeId());
        if (!exch.hasSuccessfulTasks()) {
            throw new RuntimeException("Cannot revert an exchange with no successful stages");
        }
        var appExchanges = exchanges.values().stream()
                .filter(e -> e.getAppId().equals(exch.getAppId()))
                .toList()
                .reversed();
        for (Exchange appExchange : appExchanges) {
            if (appExchange == exch)
                break;
            if (appExchange.hasSuccessfulTasks())
                throw new RuntimeException("Reversion is only applicable to the last exchange with successful stages");
        }
        exch.setStatus(ExchangeStatus.REVERTED);
    }

    @Override
    public List<String> failExpiredExchanges() {
        var expired = new ArrayList<String>();
        exchanges.values().forEach(exch -> {
            if (exch.isRunning() && exch.getLastHeartBeatAt() < System.currentTimeMillis() - TIMEOUT) {
                exch.fail("Timeout");
                expired.add(exch.getId());
            }
        });
        return expired;
    }

    @Override
    public void sendHeartBeat(String id) {
        var exch = Objects.requireNonNull(exchanges.get(id), "Exchange not found: " + id);
        if (exch.isRunning())
            throw new IllegalStateException("Exchange is not running: " + id +
                    " (status: " + exch.getStatus() + ")");
        exch.setLastHeartBeatAt(System.currentTimeMillis());
    }

    @Override
    public boolean isGenerating(IsGeneratingRequest request) {
        for (Exchange exch : exchanges.values()) {
            if (exch.getAppId().equals(request.appId()) && exch.isRunning())
                return true;
        }
        return false;
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
