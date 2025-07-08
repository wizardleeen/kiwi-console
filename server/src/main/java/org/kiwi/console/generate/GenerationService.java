package org.kiwi.console.generate;

import feign.Feign;
import feign.RequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.generate.event.GenerationListener;
import org.kiwi.console.generate.rest.CancelRequest;
import org.kiwi.console.generate.rest.RetryRequest;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.util.*;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.kiwi.console.generate.CodeSanitizer.sanitizeCode;
import static org.kiwi.console.util.Constants.*;

@Slf4j
public class GenerationService {

    public static final String NEW_APP_NAME = "New Application";
    private static final String example = TextUtil.indent(loadResource("/prompt/src/example.kiwi"));
    private static final String pageCreatePrompt = loadResource("/prompt/page-create.md");
    private static final String pageUpdatePrompt = loadResource("/prompt/page-update.md");
    private static final String kiwiCreatePrompt = Format.formatKeyed(loadResource("/prompt/kiwi-create.md"), "example", example);
    private static final String kiwiUpdatePrompt = Format.formatKeyed(loadResource("/prompt/kiwi-update.md"), "example", example);
    private static final String pageFixPrompt = loadResource("/prompt/page-fix.md");
    private static final String kiwiFixPrompt = loadResource("/prompt/kiwi-fix.md");
    private static final String planPrompt = loadResource("/prompt/plan.md");

    private final Agent agent;
    private final KiwiCompiler kiwiCompiler;
    private final PageCompiler pageCompiler;
    private final AppClient appClient;
    private final String productUrlTempl;
    private final String mgmtUrlTempl;
    private final ExchangeClient exchClient;
    private final UserClient userClient;
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
        this.taskExecutor = taskExecutor;
    }

    public static String loadResource(String file) {
        try (var input = GenerationService.class.getResourceAsStream(file)) {
            return new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void generate(String appId,
                         String prompt,
                         String userId,
                         boolean skipPageGeneration,
                         GenerationListener listener) {
        boolean creating;
        if (appId != null)
            creating = false;
        else {
            appId = createApp(NEW_APP_NAME, userId);
            creating = true;
        }
        var sysAppId = getApp(appId).getSystemAppId();
        var exchange = Exchange.create(appId, userId, prompt, creating, skipPageGeneration);
        exchange.setId(exchClient.save(exchange));
        var showAttempt = userClient.shouldShowAttempts(new UserIdRequest(userId));
        var task = new Task(exchange, sysAppId, showAttempt, listener);
        task.sendProgress();
        taskExecutor.execute(() -> runTask(task));
    }

    private void runTask(Task task) {
        try {
            var sysAppId = task.sysAppId;
            kiwiCompiler.reset(sysAppId);
            pageCompiler.reset(sysAppId);
            runningTasks.put(task.exchange.getId(), task);
            var plan = plan(task);
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

    public void reconnect(String exchangeId, GenerationListener listener) {
        var task = runningTasks.get(exchangeId);
        if (task == null)
            throw new BusinessException(ErrorCode.TASK_NOT_RUNNING);
        task.changeListener(listener);
        task.sendProgress();
    }

    public void cancel(CancelRequest request) {
        exchClient.cancel(new ExchangeCancelRequest(request.exchangeId()));
        var task = runningTasks.get(request.exchangeId());
        if (task != null)
            task.cancel();
    }

    public void retry(String userId, RetryRequest request, GenerationListener listener) {
        exchClient.retry(new ExchangeRetryRequest(request.exchangeId()));
        var exch = exchClient.get(request.exchangeId());
        var app = appClient.get(exch.getAppId());
        var showAttempts = userClient.shouldShowAttempts(new UserIdRequest(userId));
        var task = new Task(exch, app.getSystemAppId(), showAttempts, listener);
        taskExecutor.execute(() -> runTask(task));
    }

    private record Plan(boolean generateKiwi, boolean generatePage) {

        public static final Plan none = new Plan(false, false);

        public static final Plan kiwiOnly = new Plan(true, false);

        public static final Plan pageOnly = new Plan(false, true);

        public static final Plan all = new Plan(true, true);

        @Override
        public String toString() {
            return "Plan{" +
                    "generateKiwi=" + generateKiwi +
                    ", generatePage=" + generatePage +
                    '}';
        }
    }

    private Plan plan(Task task) {
        var exch = task.exchange;
        if (exch.isSkipPageGeneration())
            return task.isStageSuccessful(StageType.BACKEND) ? Plan.none : Plan.kiwiOnly;
        if (exch.isFirst()) {
            if (task.isStageSuccessful(StageType.FRONTEND))
                return Plan.none;
            if (task.isStageSuccessful(StageType.BACKEND))
                return Plan.pageOnly;
            return Plan.all;
        }
        var kiwiCode = kiwiCompiler.getCode(task.sysAppId, MAIN_KIWI);
        var pageCode = pageCompiler.getCode(task.sysAppId, APP_TSX);
        var chat = agent.createChat();
        var buf = new StringBuilder();
        var planPrompt = createPlanPrompt(exch.getPrompt(), kiwiCode, pageCode);
        log.info("Plan prompt:\n{}", planPrompt);
        chat.send(planPrompt, new ChatStreamListener() {
            @Override
            public void onThought(String thoughtChunk) {
                log.info("\n{}", thoughtChunk);
            }

            @Override
            public void onContent(String contentChunk) {
                log.info("\n{}", contentChunk);
                buf.append(contentChunk);
            }
        }, () -> task.cancelled);
        var r = Integer.parseInt(buf.toString());
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
                yield Plan.all;
            }
        };
    }

    private String createPlanPrompt(String userPrompt, String kiwiCode, String pageCode) {
        return Format.format(planPrompt, userPrompt, kiwiCode, pageCode);
    }

    private void executeGen(Runnable run) {
        for (var i = 0; i < 3; i++) {
            try {
                run.run();
                return;
            } catch (AgentException e) {
                log.error("Agent internal error", e);
            }
        }
        throw new BusinessException(ErrorCode.CODE_GENERATION_FAILED);
    }

    private void generateKiwi(Task task) {
        var sysAppId = task.sysAppId;
        var userPrompt = task.exchange.getPrompt();
        task.enterStageAndAttempt(StageType.BACKEND);
        var chat = agent.createChat();
        String prompt;
        var existingCode = kiwiCompiler.getCode(sysAppId, MAIN_KIWI);
        if (existingCode != null)
            prompt = buildUpdatePrompt(userPrompt, existingCode);
        else
            prompt = buildCreatePrompt(userPrompt);
        log.info("Kiwi generation prompt: \n{}", prompt);
        var resp = generateContent(chat, prompt, task, true);
        if (existingCode == null) {
            var appName = extractAppName(resp);
            if (appName != null)
                updateAppName(task.exchange.getAppId(), appName);
        }
        log.info("Agent Output:\n{}", resp);
        var r = runCompiler(kiwiCompiler, sysAppId, existingCode, resp, MAIN_KIWI);
        if (!r.successful()) {
            task.finishAttempt(false, r.output());
            fix(sysAppId, r.output(), task, chat, kiwiCompiler, MAIN_KIWI, kiwiFixPrompt);
        }
        else
            task.finishAttempt(true, null);
        log.info("Kiwi deployed successfully");
        kiwiCompiler.commit(sysAppId, generateKIwiCommitMsg(task));
        log.info("Kiwi source code committed");
        task.exchange.setManagementURL(Format.format(mgmtUrlTempl, sysAppId));
    }

    private DeployResult runCompiler(Compiler compiler, long appId, @Nullable String existingCode, String agentResp,  String fileName) {
        return runCompiler(compiler, appId, List.of(new Patch(fileName, existingCode, agentResp)));
    }

    private DeployResult runCompiler(Compiler compiler, long appId, List<Patch> patches) {
        var files = new ArrayList<SourceFile>();
        for (Patch patch : patches) {
            var code = patch.agentResponse;
            files.add(new SourceFile(patch.fileName, code));
        }
        return compiler.run(appId, files);
    }

    private String generateContent(Chat chat, String prompt, Task task, boolean sanitizeCode) {
        var buf = new StringBuilder();
        chat.send(prompt, new ChatStreamListener() {
            @Override
            public void onThought(String thoughtChunk) {
                task.onThought(thoughtChunk);
            }

            @Override
            public void onContent(String contentChunk) {
                task.onContent(contentChunk);
                buf.append(contentChunk);
            }
        }, () -> task.cancelled);
        return sanitizeCode ? sanitizeCode(buf.toString()) : buf.toString();
    }

    private String createApp(String name, String userId) {
        return appClient.save(App.create(name, userId));
    }

    public void updateAppName(String appId, String name) {
        appClient.updateName(new UpdateNameRequest(appId, name));
    }

    public App getApp(String id) {
        return appClient.get(id);
    }

    private @Nullable String extractAppName(String code) {
        var line = Utils.getFirstLine(code).trim();
        if (line.startsWith("//")) {
            var appname = line.substring(2).trim();
            if (!appname.isEmpty())
                return appname;
        }
        return null;
    }

    private String generateKIwiCommitMsg(Task task) {
        return task.exchange.getPrompt();
    }

    private String generatePagesCommitMsg(Task task) {
        return task.exchange.getPrompt();
    }

    private void fix(long sysAppId, String error, Task task, Chat chat, Compiler compiler, String sourceName, String promptTemplate) {
        for (int i = 0; i < 5; i++) {
            task.startAttempt();
            var code = Objects.requireNonNull(compiler.getCode(sysAppId, sourceName));
            var fixPrompt = Format.format(promptTemplate, code, error);
            log.info("Fix prompt:\n{}", fixPrompt);
            var resp = generateContent(chat, fixPrompt, task, true);
            log.info("Agent Output:\n{}", resp);
            log.info("Generated code (Retry #{}):\n{}", i + 1, resp);
            var r = runCompiler(compiler, sysAppId, code, resp, sourceName);
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
        task.enterStageAndAttempt(StageType.FRONTEND);
        var chat = agent.createChat();
        var existingCode = pageCompiler.getCode(task.sysAppId, APP_TSX);
        String prompt;
        if (existingCode == null)
            prompt = buildFrontCreatePrompt(task.exchange.getPrompt(), apiSource);
        else
            prompt = buildFrontUpdatePrompt(task.exchange.getPrompt(), existingCode, apiSource);
        log.info("Page generation prompt:\n{}", prompt);
        var resp = generateContent(chat, prompt, task, true);
        log.info("Agent Output:\n{}", resp);
        var r = runCompiler(pageCompiler, task.sysAppId, List.of(
                new Patch(APP_TSX, existingCode, resp),
                new Patch(API_TS, null, apiSource)
        ));
        if (!r.successful()) {
            task.finishAttempt(false, r.output());
            fix(task.sysAppId, r.output(), task, chat, pageCompiler, APP_TSX, pageFixPrompt);
        }
        else
            task.finishAttempt(true, null);
        log.info("Pages generated successfully");
        pageCompiler.commit(task.sysAppId, generatePagesCommitMsg(task));
        log.info("Page source code committed");
    }

    private String buildFrontUpdatePrompt(String userPrompt, String existingCode, String apiSource) {
        return Format.format(pageUpdatePrompt, userPrompt, existingCode, apiSource);
    }

    private record Patch(
            String fileName,
            @Nullable String existingCode,
            String agentResponse
    ) {}

    private String buildFrontCreatePrompt(String userPrompt, String apiSource) {
        return Format.format(pageCreatePrompt, userPrompt, apiSource);
    }

    private String buildCreatePrompt(String prompt) {
        return Format.format(kiwiCreatePrompt, prompt);
    }

    private String buildUpdatePrompt(String prompt, String code) {
        return Format.format(kiwiUpdatePrompt, prompt, code);
    }

    private class Task implements ChatStreamListener {
        private volatile GenerationListener listener;
        private Exchange exchange;
        private final long sysAppId;
        private boolean cancelled;
        private final boolean showAttempts;

        public Task(Exchange exchange, long sysAppId, boolean showAttempts, @Nonnull GenerationListener listener) {
            this.listener = listener;
            this.sysAppId = sysAppId;
            this.exchange = exchange;
            this.showAttempts = showAttempts;
            runningTasks.put(exchange.getId(), this);
        }

        void enterStageAndAttempt(StageType type) {
            if (exchange.getStatus() == ExchangeStatus.PLANNING)
                exchange.setStatus(ExchangeStatus.GENERATING);
            var stage = Stage.create(type);
            stage.addAttempt(Attempt.create());
            exchange.addStage(stage);
            saveExchange();
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
                exchange.setStatus(ExchangeStatus.FAILED);
                exchange.setErrorMessage(errorMsg);
                saveExchange();
            }
        }

        private Attempt currentAttempt() {
            return exchange.getStages().getLast().getAttempts().getLast();
        }

        private Stage currentStage() {
            return exchange.getStages().getLast();
        }

        void saveExchange() {
            ensureNotCancelled();
            exchange = exchClient.get(exchClient.save(exchange));
            sendProgress();
        }

        void sendProgress() {
            listener.onProgress(showAttempts ? exchange : exchange.clearAttempts());
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
            listener.onThought(thoughtChunk);
        }

        @Override
        public void onContent(String contentChunk) {
            listener.onContent(contentChunk);
        }

        public void changeListener(GenerationListener listener) {
            this.listener.close();
            this.listener = listener;
        }

        private void ensureNotCancelled() {
            if (cancelled)
                throw new BusinessException(ErrorCode.TASK_CANCELLED);
        }

        public void cancel() {
            cancelled = true;
        }
    }

    public static final String APP_ID = "0182d4d7b90700";

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
                new SyncTaskExecutor()
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
                null,
                "创建一个股票应用",
                USER_ID,
                false,
                printListener
        );
    }

    @SuppressWarnings("unused")
    private static void testUpdateApp(GenerationService service) {
        service.generate(
                APP_ID,
                """
                       After registering success, the web page is not automatically navigated to the login page
                       but stayed in the register page confusing the user as to whether the registration was successful.
                        """,
                USER_ID,
                true,
                printListener
        );
    }

    public static final GenerationListener printListener = new GenerationListener() {
        @Override
        public void onThought(String thoughtChunk) {
            System.out.print(thoughtChunk);
        }

        @Override
        public void onContent(String contentChunk) {
            System.out.print(contentChunk);
        }

        @Override
        public void onProgress(Exchange exchange) {

        }
    };


}
