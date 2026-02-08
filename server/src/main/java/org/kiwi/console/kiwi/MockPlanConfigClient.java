package org.kiwi.console.kiwi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MockPlanConfigClient implements PlanConfigClient {

    private final Map<String, PlanConfig> map = new HashMap<>();

    @Override
    public String create(PlanConfig planConfig) {
        var copy = copy(planConfig);
        copy.setId(UUID.randomUUID().toString());
        map.put(copy.getId(), copy);
        return copy.getId();
    }

    @Override
    public void update(String id, PlanConfig planConfig) {
        var copy = copy(planConfig);
        map.put(copy.getId(), copy);
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
