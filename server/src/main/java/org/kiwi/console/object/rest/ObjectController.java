package org.kiwi.console.object.rest;

import org.kiwi.console.kiwi.App;
import org.kiwi.console.kiwi.AppClient;
import org.kiwi.console.object.DeleteRequest;
import org.kiwi.console.object.GetRequest;
import org.kiwi.console.object.ObjectClient;
import org.kiwi.console.object.SaveRequest;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.SearchResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/object")
public class ObjectController {

    private final AppClient appClient;
    private final ObjectClient objectClient;

    public ObjectController(AppClient appClient, ObjectClient objectClient) {
        this.appClient = appClient;
        this.objectClient = objectClient;
    }

    @RequestMapping("/{id}")
    public Map<String, Object> get(
            @AuthenticationPrincipal String userId,
            @RequestHeader("X-App-Id") String appId,
            @PathVariable("id") String id) {
        var app = ensureAuthorized(userId, appId);
        return objectClient.get(new GetRequest(app.getKiwiAppId(), id));
    }

    @RequestMapping("/multi-get")
    public List<Map<String, Object>> multiGet(
            @AuthenticationPrincipal String userId,
            @RequestHeader("X-App-Id") String appId,
            @RequestBody org.kiwi.console.object.rest.MultiGetRequest request
    ) {
        var app = ensureAuthorized(userId, appId);
        return objectClient.multiGet(new org.kiwi.console.object.MultiGetRequest(
                app.getKiwiAppId(),
                request.ids(),
                request.excludeChildren(),
                request.excludeFields()
        ));
    }

    @PostMapping
    public String save(
            @AuthenticationPrincipal String userId,
            @RequestHeader("X-App-Id") String appId,
            @RequestBody org.kiwi.console.object.rest.SaveRequest request
            ) {
        var app = ensureAuthorized(userId, appId);
        return objectClient.save(new SaveRequest(
                app.getKiwiAppId(),
                request.object()
        ));
    }

    @DeleteMapping("/{id}")
    public void delete(
            @AuthenticationPrincipal String userId,
            @RequestHeader("X-App-Id") String appId,
            @PathVariable("id") String id
    ) {
        var app = ensureAuthorized(userId, appId);
        objectClient.delete(new DeleteRequest(app.getKiwiAppId(), id));
    }

    @PostMapping("/search")
    public SearchResult<Object> search(
            @AuthenticationPrincipal String userId,
            @RequestHeader("X-App-Id") String appId,
            @RequestBody SearchRequest request) {
        var app = ensureAuthorized(userId, appId);
        return objectClient.search(new org.kiwi.console.object.SearchRequest(
                app.getKiwiAppId(),
                request.type(),
                request.criteria(),
                request.newlyCreatedId(),
                request.page(),
                request.pageSize()
        ));
    }

    @PostMapping("/invoke")
    public Object invoke(
            @AuthenticationPrincipal String userId,
            @RequestHeader("X-App-Id") String appId,
            @RequestBody InvokeRequest request
    ) {
        var app = ensureAuthorized(userId, appId);
        var args = request.arguments();
        if (args == null)
            args = Map.of();
        return objectClient.invoke(new org.kiwi.console.object.InvokeRequest(
                app.getKiwiAppId(),
                request.receiver(),
                request.method(),
                args
        ));
    }

    private App ensureAuthorized(String userId, String appId) {
        var app = appClient.get(appId);
        if (!app.getMemberIds().contains(userId))
            throw new BusinessException(ErrorCode.AUTHORIZATION_FAILED);
        return app;
    }

}
