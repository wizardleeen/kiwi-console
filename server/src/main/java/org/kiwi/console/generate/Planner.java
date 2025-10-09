package org.kiwi.console.generate;

import org.kiwi.console.file.File;
import org.kiwi.console.patch.PatchReader;

import java.util.ArrayList;
import java.util.List;

public class Planner {
    private final Model model;
    private final String createTemplate;
    private final String updateTemplate;
    private final PlanAgent agent;
    private final AppGenerator appGenerator;

    public Planner(Model model, String createTemplate, String updateTemplate, PlanAgent agent, AppGenerator appGenerator) {
        this.model = model;
        this.createTemplate = createTemplate;
        this.updateTemplate = updateTemplate;
        this.agent = agent;
        this.appGenerator = appGenerator;
        appGenerator.planner = this;
    }

    public Plan plan(String requirement, List<File> attachments, boolean first) {
        var modules = new ArrayList<ModuleInfo>();
        for (ModuleGenerator moduleGenerator : appGenerator.getModuleGenerators()) {
            var mod = moduleGenerator.getModule();
            modules.add(new ModuleInfo(mod.name(), mod.tech(), mod.description(), PatchReader.buildCode(moduleGenerator.getSourceFiles())));
        }
        return agent.plan(new PlanRequest(
                model,
                createTemplate,
                updateTemplate,
                requirement,
                attachments,
                modules,
                first,
                appGenerator::isCancelled
        ));
    }

}
