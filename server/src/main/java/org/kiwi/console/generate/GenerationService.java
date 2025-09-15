package org.kiwi.console.generate;

import feign.Feign;
import feign.RequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.browser.Browser;
import org.kiwi.console.browser.PlaywrightBrowser;
import org.kiwi.console.file.File;
import org.kiwi.console.file.FileService;
import org.kiwi.console.file.UrlFetcher;
import org.kiwi.console.generate.event.GenerationListener;
import org.kiwi.console.generate.rest.CancelRequest;
import org.kiwi.console.generate.rest.ExchangeDTO;
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

import javax.annotation.Nullable;
import java.io.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.kiwi.console.util.Constants.*;

@Slf4j
public class GenerationService {

    public static final String NEW_APP_NAME = "New Application";

    private static final int MAX_AUTO_FIXES = 3;

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

    private static final GenerationListener emptyListener = new GenerationListener() {
        @Override
        public void onThought(String thoughtChunk) {

        }

        @Override
        public void onContent(String contentChunk) {

        }

        @Override
        public void onProgress(ExchangeDTO exchange) {

        }
    };

    private final Map<String, Model> models;
    private final KiwiCompiler kiwiCompiler;
    private final PageCompiler pageCompiler;
    private final AppClient appClient;
    private final UserClient userClient;
    private final String productUrlTempl;
    private final String sourceCodeUrlTempl;
    private final String mgmtUrlTempl;
    private final ExchangeClient exchClient;
    private final GenerationConfigClient generationConfigClient;
    private final UrlFetcher urlFetcher;
    private final Map<String, GenerationTask> runningTasks = new ConcurrentHashMap<>();
    private final TaskExecutor taskExecutor;
    private final Browser browser;
    private final AttachmentService attachmentService;
    private final TestAgent testAgent;

    public GenerationService(
            List<Model> models,
            KiwiCompiler kiwiCompiler,
            PageCompiler pageCompiler,
            ExchangeClient exchClient,
            AppClient appClient, UserClient userClient,
            String productUrlTempl,
            String mgmtUrlTempl,
            String sourceCodeUrlTempl,
            GenerationConfigClient generationConfigClient,
            UrlFetcher urlFetcher,
            TaskExecutor taskExecutor,
            Browser browser,
            AttachmentService attachmentService,
            TestAgent testAgent
    ) {
        this.models = models.stream().collect(Collectors.toUnmodifiableMap(Model::getName, Function.identity()));
        this.kiwiCompiler = kiwiCompiler;
        this.pageCompiler = pageCompiler;
        this.appClient = appClient;
        this.userClient = userClient;
        this.productUrlTempl = productUrlTempl;
        this.mgmtUrlTempl = mgmtUrlTempl;
        this.sourceCodeUrlTempl = sourceCodeUrlTempl;
        this.exchClient = exchClient;
        this.generationConfigClient = generationConfigClient;
        this.urlFetcher = urlFetcher;
        this.taskExecutor = taskExecutor;
        this.browser = browser;
        this.attachmentService = attachmentService;
        this.testAgent = testAgent;
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
        var exchange = Exchange.create(appId,
                userId,
                request.prompt(),
                attachmentUrls,
                creating,
                request.skipPageGeneration(),
                null,
                0,
                false);
        generate0(exchange, listener);
        return appId;
    }

    private void generate0(Exchange exchange, GenerationListener listener) {
        var attachments = Utils.map(exchange.getAttachmentUrls(), this::readAttachment);
        var app = appClient.get(exchange.getAppId());
        var user = userClient.get(exchange.getUserId());
        var genConfig = generationConfigClient.get(app.getGenConfigId());
        exchange.setId(exchClient.save(exchange));
        var task = createGenerationTask(exchange, app, user,
                genConfig,
                attachments, listener, getModel(genConfig.model()));
        task.sendProgress();
        taskExecutor.execute(() -> runTask(task));
    }

    private GenerationTask createGenerationTask(Exchange exch, App app, User user, GenerationConfig genConfig,
                                                List<File> attachments, GenerationListener listener, Model model
                                                ) {
        var task = new GenerationTask(
                exchClient, exch, app, user,false, genConfig, attachments, listener, model
        );
        runningTasks.put(exch.getId(), task);
        return task;
    }

    private Model getModel(String name) {
        return Objects.requireNonNull(models.get(name), () -> "Cannot find model: "  + name);
    }

    private File readAttachment(String url) {
        var r = urlFetcher.fetch(url);
        var mimeType = r.mimeType();
        if (!allowedMimeTypes.contains(mimeType))
            throw new BusinessException(ErrorCode.INVALID_ATTACHMENT_TYPE, "Invalid attachment type: " + mimeType);
        return new File(r.content(), r.mimeType());
    }

    void discardTask(String exchangeId) {
        var task = runningTasks.remove(exchangeId);
        if (task != null)
            task.destroy();
    }

    private void ensureNotGenerating(String appId) {
        if(exchClient.isGenerating(new IsGeneratingRequest(appId)))
            throw new BusinessException(ErrorCode.GENERATION_ALREADY_RUNNING);
    }

    private void runTask(GenerationTask task) {
        try {
            var kiwiAppId = task.app.getKiwiAppId();
            if (!task.isTesting()) {
                kiwiCompiler.reset(kiwiAppId, task.genConfig.kiwiTemplateRepo(), task.genConfig.kiwiTemplateBranch());
                pageCompiler.reset(kiwiAppId, task.genConfig.pageTemplateRepo(), task.genConfig.pageTemplateBranch());
                var plan = executeGen(() -> plan(task));
                if (task.exchange.isFirst() && plan.appName != null) {
                    updateAppName(task.app, plan.appName);
                }
                log.info("{}", plan);
                if (plan.generateKiwi) {
                    executeGen(() -> generateKiwi(task, plan.suggestion));
                }
                if (plan.generatePage) {
                    var apiSource = kiwiCompiler.generateApi(kiwiAppId);
                    executeGen(() -> generatePages(apiSource, task, plan.suggestion));
                }
                var sourceCodeUrl = task.getUser().isAllowSourceCodeDownload() ?
                        Format.format(sourceCodeUrlTempl, kiwiAppId) : null;
                task.finishGeneration(getProductUrl(kiwiAppId), sourceCodeUrl);
            }
            if (runAutoTest(task)) {
                if (task.exchange.getParentExchangeId() != null)
                    startTestIfNeeded(task.exchange.getParentExchangeId());
            }
            log.info("Generation Completed. Application: {}", task.exchange.getProductURL());
        } catch (Exception e) {
            log.error("Failed to generate code for app {}", task.exchange.getAppId(), e);
            task.abort(e.getMessage());
            throw e;
        }  finally {
            task.destroy();
            runningTasks.remove(task.exchange.getId());
        }
    }

    private String getProductUrl(long kiwiAppId) {
        return Format.format(productUrlTempl, kiwiAppId);
    }

    private void startTestIfNeeded(String exchangeId) {
        var exch = exchClient.get(exchangeId);
        if (!exch.getStages().isEmpty()) {
            var lastStage = exch.getStages().getLast();
            if (lastStage.getType() == StageType.TEST && lastStage.getStatus() == StageStatus.REJECTED) {
                startTest(exch);
            }
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

    @Scheduled(fixedDelay = 10 * 1000)
    public void sendHeartBeat() {
        for (var task : runningTasks.values()) {
            if (task.exchange.isRunning()) {
                try {
                    exchClient.sendHeartBeat(new ExchangeHeartBeatRequest(task.exchange.getId()));
                } catch (Exception e) {
                    log.warn("Failed to send heartbeat for exchange {}", task.exchange.getId(), e);
                }
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
        var user = userClient.get(userId);
        var genConfig = generationConfigClient.get(app.getGenConfigId());
        var task = createGenerationTask(exch, app, user,
                genConfig,
                attachments, listener, getModel(genConfig.model()));
        task.sendProgress();
        taskExecutor.execute(() -> runTask(task));
    }

    public void revert(String exchangeId) {
        exchClient.revert(new ExchangeIdRequest(exchangeId));
        var exch = exchClient.get(exchangeId);
        var app = appClient.get(exch.getAppId());
        var user = userClient.get(app.getOwnerId());
        if (exch.isStageSuccessful(StageType.BACKEND))
            kiwiCompiler.revert(app.getKiwiAppId(), user.isAllowSourceCodeDownload());
        if (exch.isStageSuccessful(StageType.FRONTEND))
            pageCompiler.revert(app.getKiwiAppId(),user.isAllowSourceCodeDownload() );
    }

    private record Plan(
            boolean generateKiwi,
            boolean generatePage,
            @Nullable String appName,
            @Nullable String suggestion
    ) {

        public static final Plan none = new Plan(false, false, null, null);

        public static final Plan kiwiOnly = new Plan(true, false, null, null);

        public static final Plan pageOnly = new Plan(false, true, null, null);

        public static final Plan all = new Plan(true, true, null, null);

        @Override
        public String toString() {
            return "Plan{" +
                    "generateKiwi=" + generateKiwi +
                    ", generatePage=" + generatePage +
                    '}';
        }
    }

    @SneakyThrows
    private Plan plan(GenerationTask task) {
        var exch = task.exchange;
        if (exch.isSkipPageGeneration())
            return task.isStageSuccessful(StageType.BACKEND) ? Plan.none : Plan.kiwiOnly;
        var kiwiCode = PatchReader.buildCode(kiwiCompiler.getSourceFiles(task.app.getKiwiAppId()));
        var pageCode = PatchReader.buildCode(pageCompiler.getSourceFiles(task.app.getKiwiAppId()));
        var chat = task.model.createChat(task.genConfig.outputThinking());
        var planPrompt = createPlanPrompt(exch, kiwiCode, pageCode, task);
        log.info("Plan prompt:\n{}", planPrompt);
        var text = Models.generateContent(chat, planPrompt, task.attachments, task);
        var reader = new BufferedReader(new StringReader(text));
        int r;
        try {
            r = Integer.parseInt(reader.readLine());
        } catch (NumberFormatException e) {
            throw new AgentException("Invalid plan result: " + text);
        }
        var sw = new StringWriter();
        reader.transferTo(sw);
        var content = sw.toString().trim();
        return switch (r) {
            case 0 -> Plan.none;
            case 1 -> task.isBackendSuccessful() ?
                    Plan.none : new Plan(true, false, null, content);
            case 2 -> task.isFrontendSuccessful() ?
                    Plan.none : new Plan(false, true, null, content);
            default -> {
                if (task.isFrontendSuccessful())
                    yield Plan.none;
                if (task.isBackendSuccessful())
                    yield Plan.pageOnly;
                if (task.exchange.isFirst()) {
                    yield new Plan(true, true, content, null);
                }
                yield new Plan(true, true, null, content);
            }
        };
    }

    private String createPlanPrompt(Exchange exch, String kiwiCode, String pageCode, GenerationTask task) {
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

    private void generateKiwi(GenerationTask task, String suggestion) {
        var kiwiAppId = task.app.getKiwiAppId();
        var userPrompt = task.exchange.getPrompt();
        var stageIdx = task.enterStageAndAttempt(StageType.BACKEND);
        try {
            var chat = task.model.createChat(task.genConfig.outputThinking());
            String prompt;
            var existingFiles = kiwiCompiler.getSourceFiles(kiwiAppId);
            if (!existingFiles.isEmpty())
                prompt = buildUpdatePrompt(userPrompt, suggestion, PatchReader.buildCode(existingFiles), task);
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
                fix(kiwiAppId, r.output(), task, chat, kiwiCompiler, task.genConfig.kiwiFixPrompt());
            } else
                task.finishAttempt(true, null);
            log.info("Kiwi deployed successfully");
            kiwiCompiler.commit(kiwiAppId, generateKIwiCommitMsg(task));
            log.info("Kiwi source code committed");
            task.exchange.setManagementURL(Format.format(mgmtUrlTempl, task.app.getId()));
        } catch (Exception e) {
            task.failStage(stageIdx, e.getMessage());
            throw e;
        }
    }

    private DeployResult runCompiler(Compiler compiler, long appId, List<SourceFile> files, List<Path> removedFiles, boolean deploySource) {
        return compiler.run(appId, files, removedFiles, deploySource);
    }

    private DeployResult generateCode(Chat chat, String prompt, GenerationTask task, Compiler compiler) {
        try {
            var patch = new PatchReader(Models.generateContent(chat, prompt, task.attachments, task)).read();
            return runCompiler(compiler, task.app.getKiwiAppId(), patch.addedFiles(), patch.removedFiles(), task.getUser().isAllowSourceCodeDownload());
        } catch (MalformedHunkException e) {
            return new DeployResult(false, e.getMessage());
        }
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

    private String generateKIwiCommitMsg(GenerationTask task) {
        return task.exchange.getPrompt();
    }

    private String generatePagesCommitMsg(GenerationTask task) {
        return task.exchange.getPrompt();
    }

    private void fix(long kiwiAppId, String error, GenerationTask task, Chat chat, Compiler compiler, String promptTemplate) {
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

    private void generatePages(String apiSource, GenerationTask task, String suggestion) {
        var stageIdx = task.enterStageAndAttempt(StageType.FRONTEND);
        try {
            var appId = task.app.getKiwiAppId();
            var chat = task.model.createChat(task.genConfig.outputThinking());
            var existingFiles = pageCompiler.getSourceFiles(appId);
            String prompt;
            var existingSource = PatchReader.buildCode(existingFiles);
            if (existingFiles.stream().noneMatch(f -> f.path().toString().equals(API_TS)))
                prompt = buildPageCreatePrompt(task, existingSource, apiSource);
            else
                prompt = buildPageUpdatePrompt(task, existingSource, apiSource, suggestion);
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

    private GenerationTask getTask(String exchangeId) {
        var task = runningTasks.get(exchangeId);
        if (task == null)
            throw new BusinessException(ErrorCode.TASK_NOT_RUNNING);
        return task;
    }

    @SneakyThrows
    private boolean runAutoTest(GenerationTask task) {
        var page = browser.createPage();
        page.navigate(getProductUrl(task.app.getKiwiAppId()));
        task.setPage(page);
        task.enterStageAndAttempt(StageType.TEST);
        var r = testAgent.runTest(
                task.app.getKiwiAppId(),
                page,
                getModel(task.genConfig.autoTestModel()),
                task.genConfig.autoTestPrompt(),
                task.exchange.getPrompt(),
                task
        );
        if (r.aborted())
            task.finishTest(2);
        else if (r.accepted())
            task.finishTest(0);
        else {
            task.finishTest(1);
            startFix(task, r.bugReport(), r.screenshot(), r.dom(), r.consoleLogs());
        }
        return r.accepted();
    }

    private void startFix(GenerationTask task, String bugReport, byte[] screenshot, String dom, String consoleLog) {
        if (task.exchange.getChainDepth() >= MAX_AUTO_FIXES) {
            log.info("Reached max auto fix limit. appId: {}, exchangeId: {}", task.app.getId(), task.exchange.getId());
            return;
        }
        var urls = List.of(
                attachmentService.upload("index.html", new ByteArrayInputStream(dom.getBytes(StandardCharsets.UTF_8))),
                attachmentService.upload("screenshot.png", new ByteArrayInputStream(screenshot)),
                attachmentService.upload("console-log.txt", new ByteArrayInputStream(consoleLog.getBytes(StandardCharsets.UTF_8)))
        );
        generate0(
                Exchange.create(
                        task.app.getId(), task.getUser().getId(),
                        "Fix the following issue: " + bugReport,
                        urls, false, false,
                        task.exchange.getId(),
                        task.exchange.getChainDepth() + 1,
                        false
                ),
                emptyListener);
    }

    private void startTest(Exchange exchange) {
        var testExch = Exchange.create(
                exchange.getAppId(),
                exchange.getUserId(),
                exchange.getPrompt(),
                exchange.getAttachmentUrls(),
                false,
                false,
                exchange.getParentExchangeId(),
                exchange.getChainDepth(),
                true
        );
        generate0(testExch, emptyListener);
    }

    private String buildPageUpdatePrompt(GenerationTask task, String existingCode, String apiSource, String suggestion) {
        var exch = task.exchange;
        return Format.format(task.genConfig.pageUpdatePrompt(), exch.getPrompt(), suggestion, existingCode, apiSource);
    }

    private String buildPageCreatePrompt(GenerationTask task, String existingSource, String apiSource) {
        return Format.format(task.genConfig.pageCreatePrompt(), task.app.getName(), task.exchange.getPrompt(), existingSource, apiSource);
    }

    private String buildCreatePrompt(String prompt, GenerationTask task) {
        return Format.format(task.genConfig.kiwiCreatePrompt(), prompt);
    }

    private String buildUpdatePrompt(String prompt, String suggestion, String code, GenerationTask task) {
        return Format.format(task.genConfig.kiwiUpdatePrompt(), prompt, suggestion, code);
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
        var userClient = Utils.createKiwiFeignClient(
                host,
                UserClient.class,
                CHAT_APP_ID
        );
        var appService = new AppService(host, CHAT_APP_ID, userClient);
        var service = new GenerationService(
                List.of(new GeminiModel("gemini-2.5-pro", apikey)),
                kiwiCompiler,
                new DefaultPageCompiler(java.nio.file.Path.of(PAGE_WORKDIR)),
                createFeignClient(ExchangeClient.class, CHAT_APP_ID),
                appService,
                createFeignClient(UserClient.class, CHAT_APP_ID),
                "https://{}.metavm.test",
                "http://localhost:5173/app/{}",
                "https://admin.metavm.test/source-{}.zip",
                Utils.createKiwiFeignClient(host, GenerationConfigClient.class, CHAT_APP_ID),
                new UrlFetcher("https://1000061024.metavm.test"), new SyncTaskExecutor(), new PlaywrightBrowser(),
                new AttachmentServiceImpl(
                        CHAT_APP_ID,
                        new FileService(Path.of("/Users/leen/develop/uploads")),
                        Path.of("/Users/leen/develop/sourcemap"),
                        appService
                ),
                new MockTestAgent()
            );
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
                GenerationRequest.create(null, "Create a todo list app"),
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
        public void onProgress(ExchangeDTO exchange) {

        }
    };


}
