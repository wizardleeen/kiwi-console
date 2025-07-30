package org.kiwi.console.generate.rest;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.generate.GenerationService;
import org.kiwi.console.generate.event.GenerationListener;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/generate")
@Slf4j
public class GenerationController {

    private final GenerationService generationService;
    private final AppClient appClient;
    private final ExchangeClient exchangeClient;
    private final UserClient userClient;

    public GenerationController(GenerationService generationService, AppClient appClient, ExchangeClient exchangeClient, UserClient userClient) {
        this.generationService = generationService;
        this.appClient = appClient;
        this.exchangeClient = exchangeClient;
        this.userClient = userClient;
    }

    @PostMapping
    public SseEmitter generate(@AuthenticationPrincipal String userId, @RequestBody GenerationRequest request) {
        if (request.appId() != null)
            ensureApplicationAuthorized(userId, request.appId());
        var sseEmitter = new SseEmitter(Long.MAX_VALUE);
        generationService.generate(request.appId(), request.prompt(), userId, request.skipPageGeneration(), new Emitter(sseEmitter));
        return sseEmitter;
    }

    @GetMapping("/reconnect")
    public SseEmitter reconnect(@AuthenticationPrincipal String userId, @RequestParam("exchange-id") String exchangeId) {
        ensureExchangeAuthorized(userId, exchangeId);
        var sseEmitter = new SseEmitter(Long.MAX_VALUE);
        generationService.reconnect(exchangeId, new Emitter(sseEmitter));
        return sseEmitter;
    }

    @PostMapping("/cancel")
    public void cancel(@AuthenticationPrincipal String userId, @RequestBody CancelRequest request) {
        ensureExchangeAuthorized(userId, request.exchangeId());
        generationService.cancel(request);
    }

    @PostMapping("/retry")
    public SseEmitter retry(@AuthenticationPrincipal String userId, @RequestBody RetryRequest request) {
        var sseEmitter = new SseEmitter(Long.MAX_VALUE);
        ensureExchangeAuthorized(userId, request.exchangeId());
        generationService.retry(userId, request, new Emitter(sseEmitter));
        return sseEmitter;
    }

    @PostMapping("/revert")
    public void revert(@AuthenticationPrincipal String userId, @RequestBody RevertRequest request) {
        ensureExchangeAuthorized(userId, request.exchangeId());
        generationService.revert(request.exchangeId());
    }

    @PostMapping("/history")
    public SearchResult<Exchange> search(@AuthenticationPrincipal String userId, @RequestBody HistoryRequest request) {
        if (request.appId() == null)
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        var app = appClient.get(request.appId());
        if (!app.getMemberIds().contains(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        var innerReq = new ExchangeSearchRequest(
                null,
                request.appId(),
                null,
                true,
                request.page() > 0 ? request.page() : 1,
                request.pageSize() > 0 ? request.pageSize() : 20
        );
        var r = exchangeClient.search(innerReq);
        return new SearchResult<>(
                Utils.map(r.items(), Exchange::clearDetails),
                r.total()
        );
    }


    private void ensureExchangeAuthorized(String userId, String exchangeId) {
        var exchange = exchangeClient.get(exchangeId);
        ensureApplicationAuthorized(userId, exchange.getAppId());
    }

    private void ensureApplicationAuthorized(String userId, String appId) {
        var app = appClient.get(appId);
        if (!app.getMemberIds().contains(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private record Emitter(SseEmitter sseEmitter) implements GenerationListener {

        @Override
        public void onThought(String thoughtChunk) {
        }

        @Override
        public void onContent(String contentChunk) {
        }

        @SneakyThrows
        @Override
        public void onProgress(Exchange exchange) {
            try {
                sseEmitter.send(SseEmitter.event().name("generation").data(exchange));
            }
            catch (Exception e) {
                log.error("Error sending SSE event", e);
            }
        }

        @Override
        public void close() {
            try {
                sseEmitter.complete();
            }
            catch (Exception e) {
                log.error("Error completing SSE emitter", e);
            }
        }
    }

}
