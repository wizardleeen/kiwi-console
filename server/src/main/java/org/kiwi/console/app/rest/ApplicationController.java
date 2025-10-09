package org.kiwi.console.app.rest;

import org.kiwi.console.generate.CodeAgent;
import org.kiwi.console.kiwi.AppSearchRequest;
import org.kiwi.console.kiwi.Module;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/app")
public class ApplicationController {

    private final AppClient appClient;
    private final Map<Tech, CodeAgent> codeAgentMap;

    public ApplicationController(AppClient appClient, List<CodeAgent> codeAgents) {
        this.appClient = appClient;
        codeAgentMap = codeAgents.stream().collect(Collectors.toMap(CodeAgent::getTech, Function.identity()));
    }

    @PostMapping("/search")
    public SearchResult<AppDTO> search(@AuthenticationPrincipal String userId, @RequestBody org.kiwi.console.app.rest.AppSearchRequest request) {
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
                request.newlyCreatedId()
        );
        var r = appClient.search(modifiedRequest);
        return new SearchResult<>(
                Utils.map(r.items(), App::toDTO),
                r.total()
        );
    }

    @PostMapping("/{appId}/module")
    public ModuleDTO saveModule(@PathVariable("appId") String appId, @RequestBody ModuleDTO module) {
        var app = appClient.get(appId);
        for (String dependencyId : module.dependencyIds()) {
            var dep  = Utils.find(app.getModules(), m -> Objects.equals(module.id(), m.getId()));
            if (dep == null)
                throw new BusinessException(ErrorCode.OBJECT_NOT_FOUND, "Dependency", dependencyId);
        }
        if (module.id() == null) {
            var type = Tech.valueOf(module.type());
            var countSameType = Utils.count(app.getModules(), mod -> mod.getTech() == type);
            app.addModule(Module.create(
                    module.name(),
                    countSameType == 0 ?
                            app.getKiwiAppId() + "" :
                            app.getKiwiAppId() +  "" + countSameType,
                    type,
                    module.description(),
                    module.configId()

            ));
        } else {
            var mod = Utils.find(app.getModules(), m -> Objects.equals(module.id(), m.getId()));
            if (mod == null)
                throw new BusinessException(ErrorCode.OBJECT_NOT_FOUND, "Module", module.id());
            mod.setName(module.name());
            mod.setTypeId(module.configId());
            mod.setDescription(module.description());
            mod.setDependencyIds(module.dependencyIds());
        }
        if (app.hasDependencyCircle())
            throw new BusinessException(ErrorCode.DEPENDENCY_CIRCLE);
        appClient.save(app);
        app = appClient.get(appId);
        return app.getModules().getLast().toDTO();
    }


    @PostMapping
    public String save(@AuthenticationPrincipal String userId, @RequestBody AppDTO appDTO) {
        if (appDTO.id() == null)
            return appClient.create(new CreateAppRequest(appDTO.name(), -1, userId));
        else {
            var app = appClient.get(appDTO.id());
            if (!app.getOwnerId().equals(userId))
                throw new BusinessException(ErrorCode.FORBIDDEN);
            app.setName(appDTO.name());
            return appClient.save(app);
        }
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal String userId, @PathVariable("id") String id) {
        var app = appClient.get(id);
        if (!app.getOwnerId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        for (Module module : app.getModules()) {
            Objects.requireNonNull(codeAgentMap.get(module.getTech())).delete(module.getProjectName());
        }
        appClient.delete(new DeleteAppRequest(id));
    }

    @GetMapping("/{id}")
    public AppDTO get(@AuthenticationPrincipal String userId, @PathVariable("id") String id) {
        var app = appClient.get(id);
        if (!app.getOwnerId().equals(userId) && !app.getMemberIds().contains(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        else
            return app.toDTO();
    }

}
