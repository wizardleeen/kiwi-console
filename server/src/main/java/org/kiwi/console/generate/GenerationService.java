package org.kiwi.console.generate;

import feign.Feign;
import feign.RequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.file.File;
import org.kiwi.console.file.UrlFetcher;
import org.kiwi.console.generate.event.GenerationListener;
import org.kiwi.console.generate.rest.CancelRequest;
import org.kiwi.console.generate.rest.GenerationRequest;
import org.kiwi.console.generate.rest.RetryRequest;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.patch.PatchReader;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.Utils;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import static org.kiwi.console.util.Constants.*;

@Slf4j
public class GenerationService {

    public static final String NEW_APP_NAME = "New Application";
    private static final Set<String> allowedMimeTypes = Set.of(
            "application/pdf",
            "application/json",
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp",
            "text/plain",
            "text/html",
            "video/mp4"
    );

    private final Agent agent;
    private final KiwiCompiler kiwiCompiler;
    private final PageCompiler pageCompiler;
    private final AppClient appClient;
    private final String productUrlTempl;
    private final String mgmtUrlTempl;
    private final ExchangeClient exchClient;
    private final GenerationConfigClient generationConfigClient;
    private final UserClient userClient;
    private final UrlFetcher urlFetcher;
    private final Map<String, Task> runningTasks = new ConcurrentHashMap<>();
    private final TaskExecutor taskExecutor;

    public GenerationService(
            Agent agent,
            KiwiCompiler kiwiCompiler,
            PageCompiler pageCompiler,
            ExchangeClient exchClient,
            AppClient appClient,
            UserClient userClient, String productUrlTempl,
            String mgmtUrlTempl,
            GenerationConfigClient generationConfigClient,
            UrlFetcher urlFetcher,
            TaskExecutor taskExecutor
    ) {
        this.agent = agent;
        this.kiwiCompiler = kiwiCompiler;
        this.pageCompiler = pageCompiler;
        this.appClient = appClient;
        this.productUrlTempl = productUrlTempl;
        this.mgmtUrlTempl = mgmtUrlTempl;
        this.exchClient = exchClient;
        this.userClient = userClient;
        this.generationConfigClient = generationConfigClient;
        this.urlFetcher = urlFetcher;
        this.taskExecutor = taskExecutor;
    }

    public String generate(GenerationRequest request, String userId, GenerationListener listener) {
        boolean creating;
        String appId;
        if (request.appId() != null) {
            appId = request.appId();
            creating = false;
            ensureNotGenerating(request.appId());
        } else {
             appId = createApp(NEW_APP_NAME, userId);
            creating = true;
        }
        List<String> attachmentUrls = Objects.requireNonNullElse(request.attachmentUrls(), List.of());
        var attachments = Utils.map(attachmentUrls, this::readAttachment);
        var exchange = Exchange.create(appId, userId, request.prompt(), attachmentUrls, creating, request.skipPageGeneration());
        var app = appClient.get(appId);
        exchange.setId(exchClient.save(exchange));
        var task = new Task(exchange, app, false,
                generationConfigClient.get(app.getGenConfigId()),
                attachments, listener);
        task.sendProgress();
        taskExecutor.execute(() -> runTask(task));
        return appId;
    }

    private File readAttachment(String url) {
        var r = urlFetcher.fetch(url);
        var mimeType = r.mimeType();
        if (!allowedMimeTypes.contains(mimeType))
            throw new BusinessException(ErrorCode.INVALID_ATTACHMENT_TYPE, "Invalid attachment type: " + mimeType);
        return new File(r.content(), r.mimeType());
    }

    void discardTask(String exchangeId) {
        runningTasks.remove(exchangeId);
    }

    private void ensureNotGenerating(String appId) {
        if(exchClient.isGenerating(new IsGeneratingRequest(appId)))
            throw new BusinessException(ErrorCode.GENERATION_ALREADY_RUNNING);
    }

    private void runTask(Task task) {
        try {
            var sysAppId = task.app.getSystemAppId();
            kiwiCompiler.reset(sysAppId, task.genConfig.kiwiTemplateRepo(), task.genConfig.kiwiTemplateBranch());
            pageCompiler.reset(sysAppId, task.genConfig.pageTemplateRepo(), task.genConfig.pageTemplateBranch());
            var plan = executeGen(() -> plan(task));
            if (task.exchange.isFirst() && plan.appName != null) {
                updateAppName(task.app, plan.appName);
            }
            log.info("{}", plan);
            if (plan.generateKiwi) {
                executeGen(() -> generateKiwi(task));
            }
            if (plan.generatePage) {
                var apiSource = kiwiCompiler.generateApi(sysAppId);
                executeGen(() -> generatePages(apiSource, task));
            }
            var url = Format.format(productUrlTempl, sysAppId);
            task.finish(url);
            log.info("Generation Completed. Application: {}", url);
        } catch (Exception e) {
            log.error("Failed to generate code for app {}", task.exchange.getAppId(), e);
            task.abort(e.getMessage());
            throw e;
        } finally {
            runningTasks.remove(task.exchange.getId());
        }

    }

    @Scheduled(fixedDelay = 60 * 1000)
    public void failExpiredExchanges() {
        var expiredIds = exchClient.failExpiredExchanges();
        for (String id : expiredIds) {
            var task = runningTasks.get(id);
            if (task != null) {
                task.reloadExchange();
                task.sendProgress();
            }
        }
    }

    public void reconnect(String exchangeId, GenerationListener listener) {
        var task = runningTasks.get(exchangeId);
        if (task == null)
            throw new BusinessException(ErrorCode.TASK_NOT_RUNNING);
        task.addListener(listener);
        task.sendProgress();
    }

    public void cancel(CancelRequest request) {
        exchClient.cancel(new ExchangeCancelRequest(request.exchangeId()));
        var task = runningTasks.get(request.exchangeId());
        if (task != null)
            task.cancel();
    }

    public void retry(String userId, RetryRequest request, GenerationListener listener) {
        var exch = exchClient.get(request.exchangeId());
        var app = appClient.get(exch.getAppId());
        ensureNotGenerating(app.getId());
        exchClient.retry(new ExchangeIdRequest(request.exchangeId()));
        var attachments = Utils.map(exch.getAttachmentUrls(), this::readAttachment);
        exch = exchClient.get(request.exchangeId());  // Reload
        var task = new Task(exch, app, false,
                generationConfigClient.get(app.getGenConfigId()),
                attachments, listener);
        task.sendProgress();
        taskExecutor.execute(() -> runTask(task));
    }

    public void revert(String exchangeId) {
        exchClient.revert(new ExchangeIdRequest(exchangeId));
        var exch = exchClient.get(exchangeId);
        var app = appClient.get(exch.getAppId());
        if (exch.isStageSuccessful(StageType.BACKEND))
            kiwiCompiler.revert(app.getSystemAppId());
        if (exch.isStageSuccessful(StageType.FRONTEND))
            pageCompiler.revert(app.getSystemAppId());
    }

    private record Plan(boolean generateKiwi, boolean generatePage, @Nullable String appName) {

        public static final Plan none = new Plan(false, false, null);

        public static final Plan kiwiOnly = new Plan(true, false, null);

        public static final Plan pageOnly = new Plan(false, true, null);

        public static final Plan all = new Plan(true, true, null);

        @Override
        public String toString() {
            return "Plan{" +
                    "generateKiwi=" + generateKiwi +
                    ", generatePage=" + generatePage +
                    '}';
        }
    }

    @SneakyThrows
    private Plan plan(Task task) {
        var exch = task.exchange;
        if (exch.isSkipPageGeneration())
            return task.isStageSuccessful(StageType.BACKEND) ? Plan.none : Plan.kiwiOnly;
        var kiwiCode = PatchReader.buildCode(kiwiCompiler.getSourceFiles(task.app.getSystemAppId()));
        var pageCode = PatchReader.buildCode(pageCompiler.getSourceFiles(task.app.getSystemAppId()));
        var chat = agent.createChat();
        var planPrompt = createPlanPrompt(exch, kiwiCode, pageCode, task);
        log.info("Plan prompt:\n{}", planPrompt);
        var text = generateContent(chat, planPrompt, task);
        var reader = new BufferedReader(new StringReader(text));
        int r;
        try {
            r = Integer.parseInt(reader.readLine());
        } catch (NumberFormatException e) {
            throw new AgentException("Invalid plan result: " + text);
        }
        return switch (r) {
            case 0 -> Plan.none;
            case 1 -> task.isBackendSuccessful() ?
                    Plan.none : Plan.kiwiOnly;
            case 2 -> task.isFrontendSuccessful() ?
                    Plan.none : Plan.pageOnly;
            default -> {
                if (task.isFrontendSuccessful())
                    yield Plan.none;
                if (task.isBackendSuccessful())
                    yield Plan.pageOnly;
                if (task.exchange.isFirst()) {
                    yield new Plan(true, true, reader.readLine());
                }
                yield Plan.all;
            }
        };
    }

    private String createPlanPrompt(Exchange exch, String kiwiCode, String pageCode, Task task) {
        if (exch.isFirst())
            return Format.format(task.genConfig.createAnalyzePrompt(), exch.getPrompt());
        else
            return Format.format(task.genConfig.updateAnalyzePrompt(), exch.getPrompt(), kiwiCode, pageCode);
    }

    private void executeGen(Runnable run) {
        executeGen(() -> {
            run.run();
            return null;
        });
    }

    @SneakyThrows
    private <R> R executeGen(Supplier<R> run) {
        var wait = 20;
        for (var i = 0; i < 5; i++) {
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

    private void generateKiwi(Task task) {
        var sysAppId = task.app.getSystemAppId();
        var userPrompt = task.exchange.getPrompt();
        var stageIdx = task.enterStageAndAttempt(StageType.BACKEND);
        try {
            var chat = agent.createChat();
            String prompt;
            var existingFiles = kiwiCompiler.getSourceFiles(sysAppId);
            if (!existingFiles.isEmpty())
                prompt = buildUpdatePrompt(userPrompt, PatchReader.buildCode(existingFiles), task);
            else
                prompt = buildCreatePrompt(userPrompt, task);
            log.info("Kiwi generation prompt: \n{}", prompt);
            var r = generateCode(chat, prompt, task, kiwiCompiler);
//        if (existingFiles == null) {
//            var appName = extractAppName(resp);
//            if (appName != null)
//                updateAppName(task.exchange.getAppId(), appName);
//        }
            if (!r.successful()) {
                task.finishAttempt(false, r.output());
                fix(sysAppId, r.output(), task, chat, kiwiCompiler, task.genConfig.kiwiFixPrompt());
            } else
                task.finishAttempt(true, null);
            log.info("Kiwi deployed successfully");
            kiwiCompiler.commit(sysAppId, generateKIwiCommitMsg(task));
            log.info("Kiwi source code committed");
            task.exchange.setManagementURL(Format.format(mgmtUrlTempl, sysAppId));
        } catch (Exception e) {
            task.failStage(stageIdx, e.getMessage());
            throw e;
        }
    }

    private DeployResult runCompiler(Compiler compiler, long appId, List<SourceFile> files, List<Path> removedFiles) {
        return compiler.run(appId, files, removedFiles);
    }

    private DeployResult generateCode(Chat chat, String prompt, Task task, Compiler compiler) {
        try {
            var patch = new PatchReader(generateContent(chat, prompt, task)).read();
            return runCompiler(compiler, task.app.getSystemAppId(), patch.addedFiles(), patch.removedFiles());
        } catch (MalformedHunkException e) {
            return new DeployResult(false, e.getMessage());
        }
    }

    private String generateContent(Chat chat, String prompt, Task task) {
        var buf = new StringBuilder();
        chat.send(prompt, task.attachments, new ChatStreamListener() {
            @Override
            public void onThought(String thoughtChunk) {
                task.onThought(thoughtChunk);
                task.sendHeartBeatIfRequired();
            }

            @Override
            public void onContent(String contentChunk) {
                task.onContent(contentChunk);
                task.sendHeartBeatIfRequired();
                buf.append(contentChunk);
            }
        }, () -> task.cancelled);
        if (buf.isEmpty())
            throw new AgentException("No content generated");
        return buf.toString();
    }

    private String createApp(String name, String userId) {
        return appClient.save(App.create(name, userId));
    }

    public void updateAppName(App app, String name) {
        appClient.updateName(new UpdateNameRequest(app.getId(), name));
        app.setName(name);
    }

    public App getApp(String id) {
        return appClient.get(id);
    }

    private String generateKIwiCommitMsg(Task task) {
        return task.exchange.getPrompt();
    }

    private String generatePagesCommitMsg(Task task) {
        return task.exchange.getPrompt();
    }

    private void fix(long sysAppId, String error, Task task, Chat chat, Compiler compiler, String promptTemplate) {
        for (int i = 0; i < 5; i++) {
            task.startAttempt();
            var fixPrompt = Format.format(promptTemplate, error);
            log.info("Fix prompt:\n{}", fixPrompt);
            var r = generateCode(chat, fixPrompt, task, compiler);
            if (r.successful()) {
                task.finishAttempt(true, null);
                return;
            }
            error = r.output();
            task.finishAttempt(false, error);
        }
        throw new RuntimeException("Failed to fix compilation errors after 5 attempts: " + error);
    }

    private void generatePages(String apiSource, Task task) {
        var stageIdx = task.enterStageAndAttempt(StageType.FRONTEND);
        try {
            var appId = task.app.getSystemAppId();
            var chat = agent.createChat();
            var existingFiles = pageCompiler.getSourceFiles(appId);
            String prompt;
            var existingSource = PatchReader.buildCode(existingFiles);
            if (existingFiles.stream().noneMatch(f -> f.path().toString().equals(API_TS)))
                prompt = buildPageCreatePrompt(task, existingSource, apiSource);
            else
                prompt = buildPageUpdatePrompt(task,existingSource , apiSource);
            log.info("Page generation prompt:\n{}", prompt);
            pageCompiler.addFile(appId, new SourceFile(Path.of(API_TS), apiSource));
            var r = generateCode(chat, prompt, task, pageCompiler);
            if (!r.successful()) {
                task.finishAttempt(false, r.output());
                fix(appId, r.output(), task, chat, pageCompiler, task.genConfig.pageFixPrompt());
            } else
                task.finishAttempt(true, null);
            log.info("Pages generated successfully");
            pageCompiler.commit(appId, generatePagesCommitMsg(task));
            log.info("Page source code committed");
        } catch (Exception e) {
            task.failStage(stageIdx, e.getMessage());
            throw e;
        }
    }

    private String buildPageUpdatePrompt(Task task, String existingCode, String apiSource) {
        var exch = task.exchange;
        return Format.format(task.genConfig.pageUpdatePrompt(), exch.getPrompt(), existingCode, apiSource);
    }

    private String buildPageCreatePrompt(Task task, String existingSource, String apiSource) {
        return Format.format(task.genConfig.pageCreatePrompt(), task.app.getName(), task.exchange.getPrompt(), existingSource, apiSource);
    }

    private String buildCreatePrompt(String prompt, Task task) {
        return Format.format(task.genConfig.kiwiCreatePrompt(), prompt);
    }

    private String buildUpdatePrompt(String prompt, String code, Task task) {
        return Format.format(task.genConfig.kiwiUpdatePrompt(), prompt, code);
    }

    private class Task implements ChatStreamListener {
        private final List<GenerationListener> listeners = new CopyOnWriteArrayList<>();
        private Exchange exchange;
        private final App app;
        private boolean cancelled;
        private final GenerationConfig genConfig;
        private final boolean showAttempts;
        private final List<File> attachments;

        public Task(Exchange exchange, App app, boolean showAttempts, GenerationConfig genConfig, List<File> attachments, @Nonnull GenerationListener listener) {
            this.genConfig = genConfig;
            this.attachments = attachments;
            listeners.add(listener);
            this.app = app;
            this.exchange = exchange;
            this.showAttempts = showAttempts;
            runningTasks.put(exchange.getId(), this);
        }

        int enterStageAndAttempt(StageType type) {
            if (exchange.getStatus() == ExchangeStatus.PLANNING)
                exchange.setStatus(ExchangeStatus.GENERATING);
            var stage = Stage.create(type);
            stage.addAttempt(Attempt.create());
            var stageIdx = exchange.getStages().size();
            exchange.addStage(stage);
            saveExchange();
            return stageIdx;
        }

        void startAttempt() {
            currentStage().addAttempt(Attempt.create());
            saveExchange();
        }

        void finishAttempt(boolean successful, @Nullable String errorMsg) {
            var attempt = currentAttempt();
            if (successful) {
                attempt.setStatus(AttemptStatus.SUCCESSFUL);
                currentStage().setStatus(StageStatus.SUCCESSFUL);
            } else {
                attempt.setStatus(AttemptStatus.FAILED);
                attempt.setErrorMessage(errorMsg);
            }
            saveExchange();
        }

        void finish(String productUrl) {
            exchange.setStatus(ExchangeStatus.SUCCESSFUL);
            exchange.setProductURL(productUrl);
            saveExchange();
        }

        void abort(String errorMsg) {
            if (!cancelled) {
                exchange.fail(errorMsg);
                saveExchange();
            }
        }

        private Attempt currentAttempt() {
            return exchange.getStages().getLast().getAttempts().getLast();
        }

        private Stage currentStage() {
            return exchange.getStages().getLast();
        }

        void failStage(int stageIdx, String errMsg) {
            exchange.getStages().get(stageIdx).fail(errMsg);
            saveExchange();
        }

        void saveExchange() {
            ensureNotCancelled();
            exchange.setLastHeartBeatAt(System.currentTimeMillis());
            exchange = exchClient.get(exchClient.save(exchange));
            sendProgress();
        }

        private static final long SEND_HEART_BEAT_INTERVAL = 1000 * 10;

        void sendHeartBeatIfRequired() {
            if (exchange.isRunning() && System.currentTimeMillis() - exchange.getLastHeartBeatAt() >  SEND_HEART_BEAT_INTERVAL) {
                try {
                    exchClient.sendHeartBeat(new ExchangeHeartBeatRequest(exchange.getId()));
                    reloadExchange();
                } catch (Exception e) {
                    log.warn("Failed to send heartbeat for exchange {}", exchange.getId(), e);
                }
            }
        }

        void reloadExchange() {
            exchange = exchClient.get(exchange.getId());
        }

        void sendProgress() {
            var it = listeners.iterator();
            while (it.hasNext()) {
                var listener = it.next();
                try {
                    listener.onProgress(showAttempts ? exchange : exchange.clearDetails());
                }
                catch (Exception e) {
                    log.error("Failed to send progress", e);
                    it.remove();
                }
            }
        }

        boolean isBackendSuccessful() {
            return isStageSuccessful(StageType.BACKEND);
        }

        boolean isFrontendSuccessful() {
            return isStageSuccessful(StageType.FRONTEND);
        }

        boolean isStageSuccessful(StageType type) {
            for (Stage stage : exchange.getStages()) {
                if (stage.getType() == type && !stage.getAttempts().isEmpty()
                        && stage.getAttempts().getLast().getStatus() == AttemptStatus.SUCCESSFUL)
                    return true;
            }
            return false;
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

        public void addListener(GenerationListener listener) {
            listeners.add(listener);
        }

        private void ensureNotCancelled() {
            if (cancelled)
                throw new BusinessException(ErrorCode.TASK_CANCELLED);
        }

        public void cancel() {
            cancelled = true;
        }
    }

    public static final String APP_ID = "0196b0e0b90700";

    public static void main(String[] args) throws IOException {
        var apikeyPath = "/Users/leen/develop/gemini/apikey";
        var apikey = Files.readString(java.nio.file.Path.of(apikeyPath));
        var httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        var host = "http://localhost:8080";
        var kiwiCompiler = new DefaultKiwiCompiler(
                java.nio.file.Path.of(Constants.KIWI_WORKDIR),
                new DeployClient(host, httpClient));
        var service = new GenerationService(
                new GeminiAgent(apikey),
                kiwiCompiler,
                new DefaultPageCompiler(java.nio.file.Path.of(PAGE_WORKDIR)),
                createFeignClient(ExchangeClient.class, CHAT_APP_ID),
                new AppService(host, CHAT_APP_ID, Utils.createKiwiFeignClient(
                        host,
                        UserClient.class,
                        CHAT_APP_ID
                )),
                Utils.createKiwiFeignClient(host, UserClient.class, CHAT_APP_ID), "http://{}.metavm.test",
                "http://localhost:5173/app/{}",
                Utils.createKiwiFeignClient(host, GenerationConfigClient.class, CHAT_APP_ID),
                new UrlFetcher("https://1000061024.metavm.test")
                ,
                new SyncTaskExecutor());
//        System.out.println(kiwiCompiler.generateApi(TEST_APP_ID));
        testNewApp(service);
//        testUpdateApp(service);
    }

    private static <T> T createFeignClient(Class<T> type, long appId) {
        return createFeignClient(type, rt -> rt.header("X-App-ID", Long.toString(appId)));
    }

    private static <T> T createFeignClient(Class<T> type, RequestInterceptor interceptor) {
        return Feign.builder()
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .requestInterceptor(interceptor)
                .target(type, "http://localhost:8080");
    }



    @SneakyThrows
    private static void testNewApp(GenerationService service) {
        service.generate(
                GenerationRequest.create(null, "Create a stock trading system"),
                USER_ID,
                printListener
        );
    }

    @SuppressWarnings("unused")
    private static void testUpdateApp(GenerationService service) {
        service.generate(
                GenerationRequest.create(APP_ID, "Remove the workstation feature"),
                USER_ID,
                printListener
        );
    }

    public static final GenerationListener printListener = new GenerationListener() {
        @Override
        public void onThought(String thoughtChunk) {
        }

        @Override
        public void onContent(String contentChunk) {
        }

        @Override
        public void onProgress(Exchange exchange) {

        }
    };


}
