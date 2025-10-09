package org.kiwi.console.kiwi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MockPlanConfigClient implements PlanConfigClient {

    private final Map<String, PlanConfig> map = new HashMap<>();

    @Override
    public String save(PlanConfig planConfig) {
        var copy = copy(planConfig);
        if (copy.getId() == null)
            copy.setId(UUID.randomUUID().toString());
        map.put(copy.getId(), copy);
        return copy.getId();
    }

    @Override
    public PlanConfig get(String id) {
        var config = map.get(id);
        if (config == null)
            throw new IllegalArgumentException("PlanConfig not found: " + id);
        return copy(config);
    }

    private PlanConfig copy(PlanConfig plan) {
        return new PlanConfig(
                plan.getId(),
                plan.getName(),
                plan.getModel(),
                plan.getCreatePromptTemplate(),
                plan.getUpdatePromptTemplate()
        );
    }

}
