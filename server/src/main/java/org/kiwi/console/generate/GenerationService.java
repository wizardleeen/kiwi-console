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
import org.kiwi.console.patch.PatchApply;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.Utils;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.kiwi.console.generate.CodeSanitizer.sanitizeCode;
import static org.kiwi.console.util.Constants.*;

@Slf4j
public class GenerationService {

    public static final String NEW_APP_NAME = "New Application";
    private static final String pageCommitMsgPrompt = loadPrompt("/prompt/page-commit-msg.md");
    private static final String pageCreatePrompt = loadPrompt("/prompt/page-create.md");
    private static final String pageUpdatePrompt = loadPrompt("/prompt/page-update.md");
    private static final String kiwiCommitMsgPrompt = loadPrompt("/prompt/kiwi-commit-msg.md");
    private static final String createPrompt = loadPrompt("/prompt/kiwi-create.md");
    private static final String updatePrompt = loadPrompt("/prompt/kiwi-update.md");
    private static final String pageFixPrompt = loadPrompt("/prompt/page-fix.md");
    private static final String kiwiFixPrompt = loadPrompt("/prompt/kiwi-fix.md");
    private static final String planPrompt = loadPrompt("/prompt/plan.md");

    private final Agent agent;
    private final KiwiCompiler kiwiCompiler;
    private final PageCompiler pageCompiler;
    private final ApplicationClient applicationClient;
    private final String productUrlTemplate;
    private final String managementUrlTemplate;
    private final ExchangeClient exchangeClient;
    private final Map<String, Task> runningTasks = new ConcurrentHashMap<>();
    private final String token;
    private final TaskExecutor taskExecutor;

    public GenerationService(
            Agent agent,
            KiwiCompiler kiwiCompiler,
            PageCompiler pageCompiler,
            ExchangeClient exchangeClient,
            ApplicationClient applicationClient,
            String productUrlTemplate,
            String managementUrlTemplate,
            String token,
            TaskExecutor taskExecutor
    ) {
        this.agent = agent;
        this.kiwiCompiler = kiwiCompiler;
        this.pageCompiler = pageCompiler;
        this.applicationClient = applicationClient;
        this.productUrlTemplate = productUrlTemplate;
        this.managementUrlTemplate = managementUrlTemplate;
        this.exchangeClient = exchangeClient;
        this.token = token;
        this.taskExecutor = taskExecutor;
    }

    public static String loadPrompt(String file) {
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
        exchange.setId(exchangeClient.save(exchange));
        listener.onProgress(exchange);
        var task = new Task(exchange, sysAppId, listener);
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
            var url = Format.format(productUrlTemplate, sysAppId);
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
        listener.onProgress(task.exchange);
        task.changeListener(listener);
    }

    public void cancel(CancelRequest request) {
        exchangeClient.cancel(new ExchangeCancelRequest(request.exchangeId()));
        var task = runningTasks.get(request.exchangeId());
        if (task != null)
            task.cancel();
    }

    public void retry(RetryRequest request, GenerationListener listener) {
        exchangeClient.retry(new ExchangeRetryRequest(request.exchangeId()));
        var exch = exchangeClient.get(request.exchangeId());
        var app = applicationClient.get(exch.getAppId());
        var task = new Task(exch, app.getSystemAppId(), listener);
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
        chat.send(createPlanPrompt(exch.getPrompt(), kiwiCode, pageCode), new ChatStreamListener() {
            @Override
            public void onThought(String thoughtChunk) {
            }

            @Override
            public void onContent(String contentChunk) {
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
        task.enterStage(StageType.BACKEND);
        var chat = agent.createChat();
        String prompt;
        var existingCode = kiwiCompiler.getCode(sysAppId, MAIN_KIWI);
        if (existingCode != null)
            prompt = buildUpdatePrompt(userPrompt, LineNumAnnotator.annotate(existingCode));
        else
            prompt = buildCreatePrompt(userPrompt);
        task.startAttempt();
        log.info("Kiwi generation prompt: \n{}", prompt);
        var resp = generateContent(chat, prompt, task, true);
        if (existingCode == null) {
            var appName = extractAppName(resp);
            if (appName != null)
                updateAppName(task.exchange.getAppId(), appName);
        }
        log.info("Agent Output:\n{}", resp);
        var code = existingCode != null ? PatchApply.apply(existingCode, resp) : resp;
        var r = kiwiCompiler.run(sysAppId, token, List.of(new SourceFile(MAIN_KIWI, code)));
        if (!r.successful()) {
            task.finishAttempt(false, r.output());
            fix(sysAppId, r.output(), task, chat, kiwiCompiler, MAIN_KIWI, kiwiFixPrompt);
        }
        else
            task.finishAttempt(true, null);
        log.info("Kiwi deployed successfully");
        kiwiCompiler.commit(sysAppId, generateKIwiCommitMsg(chat, task));
        log.info("Kiwi source code committed");
        task.exchange.setManagementURL(Format.format(managementUrlTemplate, sysAppId));
        task.exitStage();
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
        return applicationClient.save(Application.create(name, userId));
    }

    public void updateAppName(String appId, String name) {
        applicationClient.updateName(new UpdateNameRequest(appId, name));
    }

    public Application getApp(String id) {
        return applicationClient.get(id);
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

    private String generateKIwiCommitMsg(Chat chat, Task task) {
        return generateContent(chat, kiwiCommitMsgPrompt, task, false);
    }

    private String generatePagesCommitMsg(Chat chat, Task task) {
        return generateContent(chat, pageCommitMsgPrompt, task, false);
    }

    private void fix(long sysAppId, String error, Task task, Chat chat, Compiler compiler, String sourceName, String promptTemplate) {
        for (int i = 0; i < 5; i++) {
            task.startAttempt();
            var code = Objects.requireNonNull(compiler.getCode(sysAppId, sourceName));
            var fixPrompt = Format.format(promptTemplate, LineNumAnnotator.annotate(code), error);
            log.info("Fix prompt:\n{}", fixPrompt);
            var resp = generateContent(chat, fixPrompt, task, true);
            log.info("Agent Output:\n{}", resp);
            code = PatchApply.apply(code, resp);
            log.info("Generated code (Retry #{}):\n{}", i + 1, code);
            var r = compiler.run(sysAppId, token, List.of(new SourceFile(sourceName, code)));
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
        task.enterStage(StageType.FRONTEND);
        var chat = agent.createChat();
        var existingCode = pageCompiler.getCode(task.sysAppId, APP_TSX);
        String prompt;
        if (existingCode == null)
            prompt = buildFrontCreatePrompt(task.exchange.getPrompt(), apiSource);
        else
            prompt = buildFrontUpdatePrompt(task.exchange.getPrompt(), LineNumAnnotator.annotate(existingCode), apiSource);
        task.startAttempt();
        log.info("Page generation prompt:\n{}", prompt);
        var resp = generateContent(chat, prompt, task, true);
        log.info("Agent Output:\n{}", resp);
        var code = existingCode != null ? PatchApply.apply(existingCode, resp) : resp;
        var r = pageCompiler.run(task.sysAppId, token, List.of(new SourceFile(APP_TSX, code), new SourceFile(API_TS, apiSource)));
        if (!r.successful()) {
            task.finishAttempt(false, r.output());
            fix(task.sysAppId, r.output(), task, chat, pageCompiler, APP_TSX, pageFixPrompt);
        }
        else
            task.finishAttempt(true, null);
        log.info("Pages generated successfully");
        pageCompiler.commit(task.sysAppId, generatePagesCommitMsg(chat, task));
        log.info("Page source code committed");
        task.exitStage();
    }

    private String buildFrontUpdatePrompt(String userPrompt, String existingCode, String apiSource) {
        return Format.format(pageUpdatePrompt, userPrompt, existingCode, apiSource);
    }

    private String buildFrontCreatePrompt(String userPrompt, String apiSource) {
        return Format.format(pageCreatePrompt, userPrompt, apiSource);
    }

    private String buildCreatePrompt(String prompt) {
        return Format.format(createPrompt, prompt);
    }

    private String buildUpdatePrompt(String prompt, String code) {
        return Format.format(updatePrompt, prompt, code);
    }

    private class Task implements ChatStreamListener {
        private volatile GenerationListener listener;
        private Exchange exchange;
        private final long sysAppId;
        private boolean cancelled;

        public Task(Exchange exchange, long sysAppId, @Nonnull GenerationListener listener) {
            this.listener = listener;
            this.sysAppId = sysAppId;
            this.exchange = exchange;
            runningTasks.put(exchange.getId(), this);
        }

        void enterStage(StageType type) {
            if (exchange.getStatus() == ExchangeStatus.PLANNING)
                exchange.setStatus(ExchangeStatus.GENERATING);
            exchange.addStage(Stage.create(type));
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
                currentStage().setStatus(StageStatus.COMMITTING);
            } else {
                attempt.setStatus(AttemptStatus.FAILED);
                attempt.setErrorMessage(errorMsg);
            }
            saveExchange();
        }

        void exitStage() {
            currentStage().setStatus(StageStatus.SUCCESSFUL);
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
            exchange = exchangeClient.get(exchangeClient.save(exchange));
            listener.onProgress(exchange);
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
        var apikey = Files.readString(Path.of(apikeyPath));
        var httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        var host = "http://localhost:8080";
        var kiwiCompiler = new DefaultKiwiCompiler(
                Path.of("/tmp/kiwi-works"),
                new DeployClient(host, httpClient));
        var service = new GenerationService(
                new GeminiAgent(apikey),
                kiwiCompiler,
                new DefaultPageCompiler(Path.of("/tmp/page-works")),
                createFeignClient(ExchangeClient.class, TEST_SYS_APP_ID),
                new ApplicationService(host, TEST_SYS_APP_ID, TOKEN),
                "http://{}.metavm.test",
                "http://localhost:8080",
                TOKEN,
                new SyncTaskExecutor()
        );
//        System.out.println(kiwiCompiler.generateApi(TEST_APP_ID));
//        testNewApp(service);
        testUpdateApp(service);
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
                true,
                printListener
        );
    }

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

//        @Override
//        public void sendEvent(GenerationEvent event) {
//
//        }

        @Override
        public void onProgress(Exchange exchange) {

        }
    };


}
