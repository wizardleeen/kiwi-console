package org.kiwi.console.app.rest;

import org.kiwi.console.kiwi.Application;
import org.kiwi.console.kiwi.ApplicationClient;
import org.kiwi.console.kiwi.ApplicationSearchRequest;
import org.kiwi.console.kiwi.DeleteAppRequest;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.SearchResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app")
public class ApplicationController {

    private final ApplicationClient applicationClient;

    public ApplicationController(ApplicationClient applicationClient) {
        this.applicationClient = applicationClient;
    }

    @PostMapping("/search")
    public SearchResult<Application> search(@AuthenticationPrincipal String userId, @RequestBody ApplicationSearchRequest request) {
        var modifiedRequest = new ApplicationSearchRequest(
                request.name(),
                userId,
                request.page(),
                request.pageSize(),
                request.newlyChangedId()
        );
        return applicationClient.search(modifiedRequest);
    }

    @PostMapping
    public String save(@AuthenticationPrincipal String userId, Application application) {
        if (application.getId() == null)
            application.setOwnerId(userId);
        else {
            var existing = applicationClient.get(application.getId());
            if (!existing.getOwnerId().equals(userId))
                throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return applicationClient.save(application);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal String userId, @PathVariable("id") String id) {
        var existing = applicationClient.get(id);
        if (!existing.getOwnerId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        applicationClient.delete(new DeleteAppRequest(id));
    }

    @GetMapping("/{id}")
    public Application get(@AuthenticationPrincipal String userId, @PathVariable("id") String id) {
        var app = applicationClient.get(id);
        if (!app.getOwnerId().equals(userId) && !app.getMembersIds().contains(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        else
            return app;
    }

}
