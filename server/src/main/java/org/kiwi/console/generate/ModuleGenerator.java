package org.kiwi.console.generate;

import org.kiwi.console.file.File;
import org.kiwi.console.generate.data.DataAgent;
import org.kiwi.console.generate.data.DataManipulationRequest;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.patch.PatchReader;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ModuleGenerator {

    private final ModuleRT module;
    private final String createPromptTpl;
    private final String updatePromptTpl;
    private final String fixPromptTpl;
    private final String testPromptTpl;
    private final String templateRepo;
    private final String branch;
    private final boolean testable;
    private final boolean deploySource;
    private final boolean outputThinking;
    private final Model codeModel;
    private final @Nullable Model testModel;
    private final CodeAgent codeAgent;
    private final DataAgent dataAgent;
    private final @Nullable TestTaskFactory testTaskFactory;
    private List<ModuleGenerator> dependencies;
    private final AppGenerator appGenerator;

    public ModuleGenerator(ModuleRT module,
                           boolean deploySource,
                           Model codeModel,
                           @Nullable Model testModel,
                           CodeAgent codeAgent, DataAgent dataAgent,
                           @Nullable TestTaskFactory testTaskFactory,
                           AppGenerator appGenerator
    ) {
        this.dataAgent = dataAgent;
        var modType = module.type();
        if (modType.isTestable()) {
            Objects.requireNonNull(modType.getTestPromptTemplate(), () -> "Test prompt template is required for testable module " + module.name());
            Objects.requireNonNull(testTaskFactory, () -> "Test model is required for testable module " + module.name());
        }
        this.module = module;
        this.createPromptTpl = modType.getCreatePromptTemplate();
        this.updatePromptTpl = modType.getUpdatePromptTemplate();
        this.fixPromptTpl = modType.getFixPromptTemplate();
        this.testPromptTpl = modType.getTestPromptTemplate();
        this.templateRepo = modType.getTemplateRepository();
        this.branch = modType.getTemplateBranch();
        this.testable = modType.isTestable();
        this.deploySource = deploySource;
        this.outputThinking = modType.isOutputThinking();
        this.codeModel = codeModel;
        this.testModel = testModel;
        this.appGenerator = appGenerator;
        this.codeAgent = codeAgent;
        this.testTaskFactory = testTaskFactory;
        appGenerator.addModuleGenerator(this);
    }

    public List<SourceFile> generateApiSources() {
        return codeAgent.generateApiSources(module.projectName());
    }

    public List<SourceFile> getSourceFiles() {
        return codeAgent.getSourceFiles(module.projectName());
    }

    void generate(String requirement, String suggestion, List<File> attachments, boolean noBackup) {
        var task = appGenerator.createExchangeTask(module, ExchangeTaskType.GENERATION);
        try {
            var apiSources = new ArrayList<SourceFile>();
            for (ModuleGenerator dependency : dependencies) {
                apiSources.addAll(dependency.generateApiSources());
            }
            codeAgent.generate(new GenerationRequest(
                    codeModel,
                    appGenerator.getAppId(),
                    module.projectName(),
                    appGenerator.getAppName(),
                    createPromptTpl,
                    updatePromptTpl,
                    fixPromptTpl,
                    requirement,
                    suggestion,
                    attachments,
                    () -> appGenerator.getExchange().getStatus() == ExchangeStatus.CANCELLED,
                    deploySource,
                    outputThinking,
                    noBackup,
                    apiSources,
                    new AttemptListener(task)
            ));
            commit();
            task.setStatus(ExchangeTaskStatus.SUCCESSFUL);
        } catch (Exception e) {
            task.fail(e.getMessage());
            throw e;
        }
    }

    public void reset() {
        codeAgent.reset(module.projectName(), templateRepo, branch);
    }

    public ModuleRT getModule() {
        return module;
    }

    public void commit() {
        codeAgent.commit(module.projectName(), appGenerator.getExchange().getRequirement());
    }

    public void deploy(boolean noBackup) {
        codeAgent.deploy(appGenerator.getAppId(), module.projectName(), deploySource, noBackup);
    }

    public void delete() {
        codeAgent.delete(module.projectName());
    }

    public void setDependencies(List<ModuleGenerator> dependencies) {
        this.dependencies = dependencies;
    }

    public List<ModuleGenerator> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public boolean isTestable() {
        return testable;
    }

    public TestTask createTestTask(String requirement, ExchangeTaskRT task) {
        return Objects.requireNonNull(testTaskFactory).createTestTask(
                appGenerator.getAppId(),
                module.projectName(),
                appGenerator.getProductUrl(module.projectName()),
                Objects.requireNonNull(testModel),
                testPromptTpl,
                requirement,
                module,
                new AttemptListener(task),
                () -> appGenerator.getExchange().getStatus() == ExchangeStatus.CANCELLED, pageId -> {
                    appGenerator.getExchange().setPageId(pageId);
                    task.setStatus(ExchangeTaskStatus.GENERATING);
                });
    }

    public void runDataTask(String requirement, Plan.DataTask dataTask) {
        var exchTask = appGenerator.getExchange().startTask(module, ExchangeTaskType.DATA);
        try {
            dataAgent.run(
                    new DataManipulationRequest(
                            module.type().getDataPromptTemplate(),
                            module.type().getDataPromptFixTemplate(),
                            appGenerator.getAppId(),
                            requirement,
                            getCode(),
                            new AttemptListener(exchTask),
                            codeModel,
                            appGenerator::isCancelled
                    )
            );
            exchTask.setStatus(ExchangeTaskStatus.SUCCESSFUL);
        } catch (Exception e) {
            exchTask.fail(e.getMessage());
            throw e;
        }
    }

    private String getCode() {
        return PatchReader.buildCode(codeAgent.getSourceFiles(module.projectName()));
    }

    private record AttemptListener(ExchangeTaskRT task) implements CodeAgentListener {
        @Override
        public void onAttemptStart() {
            task.addAttempt();
        }

        @Override
        public void onAttemptSuccess() {
            task.getLastAttempt().setStatus(AttemptStatus.SUCCESSFUL);
        }

        @Override
        public void onAttemptFailure(String error) {
            task.getLastAttempt().fail(error);
        }
    }

}

