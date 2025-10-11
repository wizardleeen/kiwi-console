package org.kiwi.console.generate;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.file.File;
import org.kiwi.console.generate.data.DataAgent;
import org.kiwi.console.generate.event.GenerationListener;
import org.kiwi.console.generate.rest.ExchangeDTO;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class AppGenerator implements Task, AbortController, CodeAgentListener {
    private static final int MAX_AUTO_FIXES = 6;
    private final List<GenerationListener> listeners = Collections.synchronizedList(new ArrayList<>());
    private final ExchangeRT exchange;
    final AppRT app;
    final String sourceCodeUrlTempl;
    final String managementUrlTempl;
    final String productUrlTempl;
    Planner planner;
    private final DataAgent dataAgent;
    private final List<ModuleGenerator> moduleGenerators = new ArrayList<>();
    final boolean showAttempts;
    final List<File> attachments;
    private final User user;
    private int numTests;
    private final Function<String, Model> getModel;
    private final Function<Tech, CodeAgent> getCodeAgent;
    private final Function<Tech, TestTaskFactory> getTestRunnerFactory;

    public AppGenerator(ExchangeRT exchange,
                        AppRT app,
                        User user,
                        boolean showAttempts,
                        List<File> attachments,
                        @Nonnull GenerationListener listener,
                        String sourceCodeUrlTempl,
                        String productUrlTempl,
                        String managementUrlTempl, DataAgent dataAgent,
                        Function<String, Model> getModel,
                        Function<Tech, CodeAgent> getCodeAgent,
                        Function<Tech, TestTaskFactory> getTestRunnerFactory
                        ) {
        this.attachments = attachments;
        this.sourceCodeUrlTempl = sourceCodeUrlTempl;
        this.productUrlTempl = productUrlTempl;
        this.managementUrlTempl = managementUrlTempl;
        this.dataAgent = dataAgent;
        listeners.add(listener);
        this.app = app;
        this.user = user;
        this.exchange = exchange;
        this.showAttempts = showAttempts;
        this.getModel = getModel;
        this.getCodeAgent = getCodeAgent;
        this.getTestRunnerFactory = getTestRunnerFactory;
        exchange.setListener(this::onProgress);
    }

    ExchangeTaskRT createExchangeTask(ModuleRT module, ExchangeTaskType type) {
        if (exchange.getStatus() == ExchangeStatus.PLANNING)
            exchange.setStatus(ExchangeStatus.GENERATING);
        return exchange.startTask(module, type);
    }

    public void run(boolean retry) {
        sendProgress();
        reset();
        var requirement = exchange.getRequirement();
        var attachments = this.attachments;
        try {
            run(requirement, attachments, false, retry);
            exchange.setStatus(ExchangeStatus.SUCCESSFUL);
        } catch (Exception e) {
            if (!exchange.isCancelled())
                exchange.abort(e.getMessage());
            throw e;
        }
    }

    private void setupDependencies() {
        var map = new HashMap<String, ModuleGenerator>();
        for (ModuleGenerator modGen : moduleGenerators) {
            map.put(modGen.getModule().id(), modGen);
        }
        for (ModuleGenerator moduleGenerator : moduleGenerators) {
            var deps = Utils.map(
                    moduleGenerator.getModule().dependencies(),
                    dep -> Objects.requireNonNull(map.get(dep.id()), () -> "Cannot find module with id " + dep.id())
            );
            moduleGenerator.setDependencies(deps);
        }
        var sorted = new ArrayList<ModuleGenerator>();
        var visiting = new HashSet<ModuleGenerator>();
        var visited = new HashSet<ModuleGenerator>();
        for (ModuleGenerator modGen : moduleGenerators) {
            sortModGens(sorted, visiting, visited, modGen);
        }
        moduleGenerators.clear();
        moduleGenerators.addAll(sorted);
    }

    private void sortModGens(List<ModuleGenerator> result, Set<ModuleGenerator> visiting, Set<ModuleGenerator> visited, ModuleGenerator current) {
        if (visited.contains(current))
            return;
        if (visiting.contains(current))
            throw new RuntimeException("Circular dependency detected at module " + current.getModule().name());
        visiting.add(current);
        var deps = current.getDependencies();
        for (ModuleGenerator dep : deps) {
            sortModGens(result, visiting, visited, dep);
        }
        visiting.remove(current);
        visited.add(current);
        result.add(current);
    }

    private boolean run(String requirement, List<File> attachments, boolean fix, boolean retry) {
        var first = exchange.isFirst() && !fix && !retry;
        var plan = executeGen(() -> plan(requirement, attachments, first));
        if (plan.appName() != null)
            app.setName(plan.appName());
        log.info("{}", plan);
        List<Plan.Task> plannedTasks;
        if (first) {
            plannedTasks = Utils.map(moduleGenerators, modGen -> new Plan.ModifyModuleTask(
                    modGen.getModule().name(), ""
            ));
        } else {
            plannedTasks = plan.tasks();
        }
        return runTasks(requirement, attachments, plannedTasks, fix);
    }

    private boolean runTasks(String requirement, List<File> attachments, List<Plan.Task> tasks, boolean noBackup) {
        try {
            var kiwiAppId = app.getKiwiAppId();
            for (Plan.Task task : tasks) {
                if (!runTask(requirement, attachments, noBackup, task))
                    return false;
            }
            var sourceCodeUrl = user.isAllowSourceCodeDownload() ?
                    Format.format(sourceCodeUrlTempl, kiwiAppId) : null;
            finishGeneration(
                    getProductUrl(kiwiAppId + ""),
                    getManagementUrl(app.getId()),
                    sourceCodeUrl
            );
            log.info("Generation Completed. Application: {}", exchange.getProductURL());
            return true;
        } catch (Exception e) {
            log.error("Failed to generate code for app {}", exchange.getAppId(), e);
            fail(e.getMessage());
            throw e;
        }  finally {
            destroy();
        }
    }

    private void runModifyModuleTask(String requirement, List<File> attachments, Plan.ModifyModuleTask task, boolean noBackup) {
        var modGen = getModuleGeneratorByName(task.moduleName());
        executeGen(() -> modGen.generate(requirement, task.suggestion(), attachments, noBackup));
    }

    private boolean runTask(String requirement, List<File> attachments, boolean noBackup, Plan.Task task) {
        return switch (task) {
            case Plan.CreateModuleTask createModuleTask -> {
                runCreateModuleTask(requirement, attachments, createModuleTask);
                yield true;
            }
            case Plan.ModifyModuleTask modifyModuleTask -> {
                runModifyModuleTask(requirement, attachments, modifyModuleTask, noBackup);
                yield true;
            }
            case Plan.DeleteModuleTask deleteModuleTask -> {
                runDeleteModuleTask(deleteModuleTask);
                yield true;
            }
            case Plan.DataTask dataTask -> {
                runDataTask(requirement, dataTask);
                yield true;
            }
            case Plan.TestTask testTask -> runTestTask(requirement, testTask);
        };
    }

    private void runCreateModuleTask(String requirement, List<File> attachments, Plan.CreateModuleTask task) {
        var modGen = createModule(task.moduleName(), task.description(), task.tech(), task.dependencyNames());
        modGen.reset();
        executeGen(() -> modGen.generate(requirement, task.suggestion(), attachments, false));
    }


    private ModuleGenerator createModule(String name, String description, Tech tech, List<String> dependencyNames) {
        var deps = Utils.map(dependencyNames, app::getModuleByName);
        var mod = app.addModule(name, description, tech, deps);
        return new ModuleGenerator(mod,
                user.isAllowSourceCodeDownload(),
                getModel.apply(mod.type().getCodeModel()),
                Utils.safeCall(mod.type().getTestModel(), getModel),
                getCodeAgent.apply(mod.tech()),
                dataAgent,
                getTestRunnerFactory.apply(mod.tech()),
                this
        );
    }

    private boolean testAndFix(String requirement, List<Plan.Task> plannedTasks) {
        for (var plannedTask : plannedTasks) {
            if (plannedTask instanceof Plan.TestTask testTask && !runTestTask(requirement, testTask))
                return false;
        }
        return true;
    }

    private boolean runTestTask(String requirement, Plan.TestTask plannedTask) {
        if (numTests >= MAX_AUTO_FIXES)
            return true;
        var modGen = getModuleGeneratorByName(plannedTask.moduleName());
        var exchTask = createExchangeTask(modGen.getModule(), ExchangeTaskType.TEST);
        var task = modGen.createTestTask(requirement, exchTask);
        while (numTests++ < MAX_AUTO_FIXES) {
            var r = task.runTest();
            if (r.rejected()) {
                exchTask.setStatus(ExchangeTaskStatus.REJECTED);
                if (!run(r.getFixRequirement(), r.getAttachments(), true, false))
                    return false;
            } else {
                task.close();
                exchTask.setStatus(r.accepted() ? ExchangeTaskStatus.SUCCESSFUL : ExchangeTaskStatus.FAILED);
                break;
            }
        }
        return true;
    }

    private void runDeleteModuleTask(Plan.DeleteModuleTask task) {
        var modGen = getModuleGeneratorByName(task.moduleName());
        modGen.delete();
        moduleGenerators.remove(modGen);
    }

    private void runDataTask(String requirement, Plan.DataTask dataTask) {
        var modGen = getModuleGeneratorByName(dataTask.moduleName());
        modGen.runDataTask(requirement, dataTask);
    }

    @SneakyThrows
    private Plan plan(String requirement, List<File> attachments, boolean first) {
        return planner.plan(
                requirement,
                attachments,
                first
        );
    }

    String getProductUrl(String prefix) {
        return Format.format(productUrlTempl, prefix);
    }

    String getManagementUrl(String prefix) {
        return Format.format(managementUrlTempl, prefix);
    }

    private void executeGen(Runnable r) {
        executeGen(() -> {
            r.run();
            return null;
        });
    }

    @SneakyThrows
    private <R> R executeGen(Supplier<R> run) {
        var wait = 1000;
        for (var i = 0; i < 6; i++) {
            try {
                return run.get();
            } catch (AgentException e) {
                log.error("Agent internal error", e);
                Thread.sleep(wait);
                wait *= 2;
            }
        }
        throw new BusinessException(ErrorCode.CODE_GENERATION_FAILED);
    }


    void addModuleGenerator(ModuleGenerator moduleGenerator) {
        moduleGenerators.add(moduleGenerator);
        setupDependencies();
    }

    void startAttempt() {
        currentStage().addAttempt();
    }

    void finishAttempt(boolean successful, @Nullable String errorMsg) {
        var attempt = currentAttempt();
        if (successful) {
            attempt.setStatus(AttemptStatus.SUCCESSFUL);
            currentStage().setStatus(ExchangeTaskStatus.SUCCESSFUL);
        } else {
            attempt.setStatus(AttemptStatus.FAILED);
            attempt.setErrorMessage(errorMsg);
        }
    }

    void finishGeneration(String productUrl, String managementUrl,String sourceCodeUrl) {
        exchange.setManagementURL(managementUrl);
        exchange.setProductURL(productUrl);
        exchange.setSourceCodeURL(sourceCodeUrl);
    }

    void fail(String errorMsg) {
        if (!exchange.isCancelled()) {
            exchange.fail(errorMsg);
        }
    }

    void destroy() {
    }

    private AttemptRT currentAttempt() {
        return exchange.getTasks().getLast().getAttempts().getLast();
    }

    private ExchangeTaskRT currentStage() {
        return exchange.getTasks().getLast();
    }

    @Override
    public List<File> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<File> attachments) {
        this.attachments.clear();
        this.attachments.addAll(attachments);
    }

    @Override
    public boolean isCancelled() {
        return exchange.isCancelled();
    }

    void reloadExchange() {
        exchange.reload();
    }

    void sendProgress() {
        exchange.sendProgress();
    }

    @Override
    public void onThought(String thoughtChunk) {
        System.out.print(thoughtChunk);
        for (var listener : listeners) {
            listener.onThought(thoughtChunk);
        }
    }

    @Override
    public void onContent(String contentChunk) {
        System.out.print(contentChunk);
        for (var listener : listeners) {
            listener.onContent(contentChunk);
        }
    }

    private void onProgress(ExchangeDTO exchange) {
        var it = listeners.iterator();
        while (it.hasNext()) {
            var l = it.next();
            try {
                l.onProgress(exchange);
            } catch (Exception e) {
                log.error("Failed to notify generation listener", e);
                it.remove();
            }
        }
    }

    public void addListener(GenerationListener listener) {
        listeners.add(listener);
    }

    public void cancel() {
        exchange.cancel();
    }

    public User getUser() {
        return user;
    }

    @Override
    public boolean isAborted() {
        return isCancelled();
    }

    @Override
    public void onAttemptStart() {
        startAttempt();
    }

    @Override
    public void onAttemptSuccess() {
        finishAttempt(true, null);
    }

    @Override
    public void onAttemptFailure(String error) {
        finishAttempt(false, error);
    }

    public ModuleGenerator getModuleGeneratorByName(String name) {
        return Utils.findRequired(moduleGenerators, mg -> mg.getModule().name().equals(name),
                () -> new IllegalStateException("No module found for name: " + name));
    }

    public void reset() {
        for (ModuleGenerator moduleGenerator : moduleGenerators) {
            moduleGenerator.reset();
        }
    }

    public List<ModuleGenerator> getModuleGenerators() {
        return moduleGenerators;
    }

    public long getAppId() {
        return app.getKiwiAppId();
    }


    public ExchangeRT getExchange() {
        return exchange;
    }

    public String getAppName() {
        return app.getName();
    }
}


