package org.kiwi.console.generate;

import feign.Feign;
import feign.RequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.file.File;
import org.kiwi.console.file.UrlFetcher;
import org.kiwi.console.generate.data.DataAgent;
import org.kiwi.console.generate.data.DataAgentImpl;
import org.kiwi.console.generate.event.GenerationListener;
import org.kiwi.console.generate.rest.CancelRequest;
import org.kiwi.console.generate.rest.ExchangeDTO;
import org.kiwi.console.generate.rest.GenerationRequest;
import org.kiwi.console.generate.rest.RetryRequest;
import org.kiwi.console.kiwi.Module;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.Utils;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private final Map<String, Model> models;
    private final PlanAgent planAgent;
    private final Map<Tech, CodeAgent> codeAgentMap;
    private final DataAgent dataAgent;
    private final Map<Tech, TestTaskFactory> testRunnerFactoryMap;
    private final AppClient appClient;
    private final ModuleTypeClient moduleTypeClient;
    private final AppConfigClient appConfigClient;
    private final PlanConfigClient planConfigClient;
    private final UserClient userClient;
    private final String productUrlTempl;
    private final String managementUrlTempl;
    private final String sourceCodeUrlTempl;
    private final ExchangeClient exchClient;
    private final UrlFetcher urlFetcher;
    private final Map<String, AppGenerator> runningTasks = new ConcurrentHashMap<>();
    private final TaskExecutor taskExecutor;

    public GenerationService(
            List<Model> models,
            PlanAgent planAgent,
            List<CodeAgent> codeAgents, DataAgent dataAgent,
            List<TestTaskFactory> testAgents,
            ExchangeClient exchClient,
            AppClient appClient,
            AppConfigClient appConfigClient,
            UserClient userClient,
            ModuleTypeClient moduleTypeClient, PlanConfigClient planConfigClient,
            String productUrlTempl,
            String managementUrlTempl,
            String sourceCodeUrlTempl,
            UrlFetcher urlFetcher,
            TaskExecutor taskExecutor
    ) {
        this.models = models.stream().collect(Collectors.toUnmodifiableMap(Model::getName, Function.identity()));
        this.planAgent = planAgent;
        this.codeAgentMap = Utils.toMap(codeAgents, CodeAgent::getTech, Function.identity());
        this.dataAgent = dataAgent;
        this.testRunnerFactoryMap = Utils.toMap(testAgents, TestTaskFactory::getTech, Function.identity());
        this.appClient = appClient;
        this.appConfigClient = appConfigClient;
        this.userClient = userClient;
        this.moduleTypeClient = moduleTypeClient;
        this.planConfigClient = planConfigClient;
        this.productUrlTempl = productUrlTempl;
        this.managementUrlTempl = managementUrlTempl;
        this.sourceCodeUrlTempl = sourceCodeUrlTempl;
        this.exchClient = exchClient;
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
        var planConfig = planConfigClient.get(app.getPlanConfigId());

        exchange.setId(exchClient.save(exchange));
        var task = createAppGenerator(exchange, app, user,
                planConfig,
                attachments, listener);
        taskExecutor.execute(() -> run(task, false));
    }

    private AppGenerator createAppGenerator(Exchange exch, App app, User user, PlanConfig planConfig,
                                            List<File> attachments, GenerationListener listener
                                                ) {
        var appRT = AppRT.from(app, appConfigClient.get(user.getAppConfigId()), moduleTypeClient, appClient);
        var gen = new AppGenerator(
                ExchangeRT.from(exch, appRT, exchClient),
                appRT,
                user,
                false,
                attachments,
                listener,
                sourceCodeUrlTempl,
                productUrlTempl,
                managementUrlTempl,
                dataAgent,
                this::getModel,
                codeAgentMap::get,
                testRunnerFactoryMap::get
        );
        new Planner(
                getModel(planConfig.getModel()),
                planConfig.getCreatePromptTemplate(),
                planConfig.getUpdatePromptTemplate(),
                planAgent,
                gen
        );
        for (var module : appRT.getModules()) {
            var modType = module.type();
            new ModuleGenerator(
                    module,
                    user.isAllowSourceCodeDownload(),
                    getModel(modType.getCodeModel()),
                    Utils.safeCall(modType.getTestModel(), this::getModel),
                    Objects.requireNonNull(codeAgentMap.get(module.tech()), () -> "Cannot find code agent for tech: " + module.tech()),
                    dataAgent,
                    testRunnerFactoryMap.get(module.tech()),
                    gen
            );
        }
        runningTasks.put(exch.getId(), gen);
        return gen;
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

    private void run(AppGenerator gen, boolean retry) {
        try {
            gen.run(retry);
        } finally {
            runningTasks.remove(gen.getExchange().getId());
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
            if (task.getExchange().isRunning()) {
                try {
                    exchClient.sendHeartBeat(task.getExchange().getId());
                } catch (Exception e) {
                    log.warn("Failed to send heartbeat for exchange {}", task.getExchange().getId(), e);
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
        exchClient.cancel(request.exchangeId());
        var task = runningTasks.get(request.exchangeId());
        if (task != null)
            task.cancel();
    }

    public void retry(String userId, RetryRequest request, GenerationListener listener) {
        var exch = exchClient.get(request.exchangeId());
        var app = appClient.get(exch.getAppId());
        ensureNotGenerating(app.getId());
        exchClient.retry(request.exchangeId());
        var attachments = Utils.map(exch.getAttachmentUrls(), this::readAttachment);
        exch = exchClient.get(request.exchangeId());  // Reload
        var user = userClient.get(userId);
        var planConfig = planConfigClient.get(app.getPlanConfigId());
        var task = createAppGenerator(exch, app, user,
                planConfig,
                attachments, listener);
        taskExecutor.execute(() -> run(task, true));
    }

    public void revert(String exchangeId) {
        exchClient.revert(new ExchangeIdRequest(exchangeId));
        var exch = exchClient.get(exchangeId);
        var app = appClient.get(exch.getAppId());
        var user = userClient.get(app.getOwnerId());
        for (Module mod : app.getModules()) {
            if (exch.isModuleSuccessful(mod.getId()))
                getCodeAgent(mod.getTech()).revert(app.getKiwiAppId(), mod.getProjectName(), user.isAllowSourceCodeDownload());
        }
    }

    private CodeAgent getCodeAgent(Tech tech) {
        return Objects.requireNonNull(codeAgentMap.get(tech), () -> "Cannot find code agent for module type: " + tech);
    }

    private String createApp(String name, String userId) {
        return appClient.create(new CreateAppRequest(name, -1, userId));
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
                List.of(new GeminiModel("gemini-2.5-pro")),
                new PlanAgent(),
                List.of(
                    new KiwiAgent(kiwiCompiler),
                    new WebAgent(new DefaultPageCompiler(java.nio.file.Path.of(PAGE_WORKDIR)))
                ),
                new DataAgentImpl("http://localhost:8080"),
                List.of(new MockTestTaskFactory()),
                createFeignClient(ExchangeClient.class, CHAT_APP_ID),
                appService,
                createFeignClient(AppConfigClient.class, CHAT_APP_ID),
                createFeignClient(UserClient.class, CHAT_APP_ID),
                createFeignClient(ModuleTypeClient.class, CHAT_APP_ID),
                createFeignClient(PlanConfigClient.class, CHAT_APP_ID),
                "https://{}.metavm.test",
                "https://metavm.test/{}",
                "https://admin.metavm.test/source-{}.zip",
                new UrlFetcher("https://1000061024.metavm.test"), new SyncTaskExecutor());
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
