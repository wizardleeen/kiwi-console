package org.kiwi.console.app.rest;

import org.kiwi.console.generate.KiwiCompiler;
import org.kiwi.console.generate.PageCompiler;
import org.kiwi.console.kiwi.App;
import org.kiwi.console.kiwi.AppClient;
import org.kiwi.console.kiwi.AppSearchRequest;
import org.kiwi.console.kiwi.DeleteAppRequest;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.SearchResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app")
public class ApplicationController {

    private final AppClient appClient;
    private final KiwiCompiler kiwiCompiler;
    private final PageCompiler pageCompiler;

    public ApplicationController(AppClient appClient, KiwiCompiler kiwiCompiler, PageCompiler pageCompiler) {
        this.appClient = appClient;
        this.kiwiCompiler = kiwiCompiler;
        this.pageCompiler = pageCompiler;
    }

    @PostMapping("/search")
    public SearchResult<App> search(@AuthenticationPrincipal String userId, @RequestBody AppSearchRequest request) {
        var page = request.page();
        var pageSize = request.pageSize();
        if (page == 0)
            page = 1;
        if (pageSize == 0)
            pageSize = 20;
        var modifiedRequest = new AppSearchRequest(
                request.name(),
                userId,
                page,
                pageSize,
                request.newlyChangedId()
        );
        return appClient.search(modifiedRequest);
    }

    @PostMapping
    public String save(@AuthenticationPrincipal String userId, App app) {
        if (app.getId() == null)
            app.setOwnerId(userId);
        else {
            var existing = appClient.get(app.getId());
            if (!existing.getOwnerId().equals(userId))
                throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return appClient.save(app);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal String userId, @PathVariable("id") String id) {
        var app = appClient.get(id);
        if (!app.getOwnerId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        pageCompiler.delete(app.getKiwiAppId());
        kiwiCompiler.delete(app.getKiwiAppId());
        appClient.delete(new DeleteAppRequest(id));
    }

    @GetMapping("/{id}")
    public App get(@AuthenticationPrincipal String userId, @PathVariable("id") String id) {
        var app = appClient.get(id);
        if (!app.getOwnerId().equals(userId) && !app.getMemberIds().contains(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        else
            return app;
    }

}
