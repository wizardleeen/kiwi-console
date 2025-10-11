package org.kiwi.console.kiwi;

import org.kiwi.console.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MockAppConfigClient implements AppConfigClient {

    private final String presetId;
    private final Map<String, AppConfig> map = new HashMap<>();

    public MockAppConfigClient(PlanConfigClient planConfigClient, ModuleTypeClient moduleTypeClient) {
        presetId = save(new AppConfig(
                UUID.randomUUID().toString(),
                "default",
                planConfigClient.save(new PlanConfig(
                        null,
                        "default",
                        "mock",
                            Utils.loadResource("/prompt/create-analyze.md"),
                            Utils.loadResource("/prompt/update-analyze.md")
                        )
                ),
                moduleTypeClient.save(new ModuleType(
                        null,
                        "kiwi",
                        "kiwi",
                        Tech.KIWI,
                        "mock",
                        null,
                        Utils.loadResource("/prompt/kiwi-create.md"),
                        Utils.loadResource("/prompt/kiwi-update.md"),
                        Utils.loadResource("/prompt/kiwi-fix.md"),
                        null,
                        Utils.loadResource("/prompt/data.md"),
                        Utils.loadResource("/prompt/data-fix.md"),
                        "",
                        "",
                        false,
                        false
                )),
                moduleTypeClient.save(new ModuleType(
                        null,
                        "web",
                        "web",
                        Tech.WEB,
                        "mock",
                        "mock",
                        Utils.loadResource("/prompt/page-create.md"),
                        Utils.loadResource("/prompt/page-update.md"),
                        Utils.loadResource("/prompt/page-fix.md"),
                        Utils.loadResource("/prompt/auto-test.md"),
                        "",
                        "",
                        "",
                        "",
                        true,
                        false
                ))
        ));
    }

    public String getPresetId() {
        return presetId;
    }

    @Override
    public String save(AppConfig appConfig) {
        var copy = copy(appConfig);
        if (copy.getId() == null)
            copy.setId(UUID.randomUUID().toString());
        map.put(copy.getId(), copy);
        return copy.getId();
    }

    @Override
    public AppConfig get(String id) {
        var config = map.get(id);
        if (config == null)
            throw new IllegalArgumentException("AppConfig not found: " + id);
        return copy(config);
    }

    private AppConfig copy(AppConfig appConfig) {
        return new AppConfig(
                appConfig.getId(),
                appConfig.getName(),
                appConfig.getPlanConfigId(),
                appConfig.getBackendModTypeId(),
                appConfig.getFrontendModTypeId()
        );
    }

}
