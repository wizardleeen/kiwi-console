package org.kiwi.console.kiwi;

import org.kiwi.console.util.SearchResult;
import org.kiwi.console.util.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MockModuleTypeClient implements ModuleTypeClient {

    private final Map<String, ModuleType> map = new HashMap<>();

    @Override
    public String save(ModuleType moduleType) {
        var copy = copy(moduleType);
        if (copy.getId() == null)
            copy.setId(UUID.randomUUID().toString());
        map.put(copy.getId(), copy);
        return copy.getId();
    }

    @Override
    public String create(ModuleType moduleType) {
        var copy = copy(moduleType);
        copy.setId(UUID.randomUUID().toString());
        map.put(copy.getId(), copy);
        return copy.getId();
    }

    @Override
    public void update(String id, ModuleType moduleType) {
        var copy = copy(moduleType);
        map.put(copy.getId(), copy);
    }

    @Override
    public ModuleType get(String id) {
        var type = map.get(id);
        if (type == null)
            throw new IllegalArgumentException("ModuleType not found: " + id);
        return copy(type);
    }

    @Override
    public SearchResult<ModuleType> multiGet(List<String> ids) {
        return new SearchResult<>(Utils.map(ids, this::get), ids.size());
    }

    private ModuleType copy(ModuleType config) {
        return new ModuleType(
                config.getId(),
                config.getName(),
                config.getDescription(),
                config.getTech(),
                config.getCodeModel(),
                config.getTestModel(),
                config.getCreatePromptTemplate(),
                config.getUpdatePromptTemplate(),
                config.getFixPromptTemplate(),
                config.getTestPromptTemplate(),
                config.getTemplateRepository(),
                config.getTemplateBranch(),
                config.getDataPromptTemplate(),
                config.getDataPromptFixTemplate(),
                config.isTestable(),
                config.isOutputThinking()
        );
    }


}
