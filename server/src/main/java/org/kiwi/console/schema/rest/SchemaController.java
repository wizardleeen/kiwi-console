package org.kiwi.console.schema.rest;

import org.kiwi.console.kiwi.App;
import org.kiwi.console.kiwi.AppClient;
import org.kiwi.console.schema.SchemaClient;
import org.kiwi.console.schema.dto.SchemaResponse;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schema")
public class SchemaController {

    private final AppClient appClient;
    private final SchemaClient schemaClient;

    public SchemaController(AppClient appClient, SchemaClient schemaClient) {
        this.appClient = appClient;
        this.schemaClient = schemaClient;
    }

    @GetMapping
    public SchemaResponse get(
            @AuthenticationPrincipal String userId,
            @RequestHeader("X-App-Id") String appId) {
        var app = ensureAuthorized(userId, appId);
        return schemaClient.get(app.getKiwiAppId());
    }


    private App ensureAuthorized(String userId, String appId) {
        var app = appClient.get(appId);
        if (!app.getMemberIds().contains(userId))
            throw new BusinessException(ErrorCode.AUTHORIZATION_FAILED);
        return app;
    }


}
