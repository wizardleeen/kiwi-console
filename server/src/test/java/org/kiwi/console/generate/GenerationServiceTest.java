package org.kiwi.console.generate;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.file.UrlFetcher;
import org.kiwi.console.file.UrlResource;
import org.kiwi.console.generate.data.DataAgent;
import org.kiwi.console.generate.data.DataManipulationRequest;
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
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.kiwi.console.util.Constants.APP_TSX;
import static org.kiwi.console.util.Constants.MAIN_KIWI;

@Slf4j
public class GenerationServiceTest extends TestCase {

    private PlanAgent planAgent;
    private KiwiAgent kiwiAgent;
    private WebAgent webAgent;
    private DataAgent dataAgent;
    private TestTaskFactory testTaskFactory;
    private AppClient appClient;
    private MockExchangeClient exchangeClient;
    private UserClient userClient;
    private PlanConfigClient planConfigClient;
    private MockAppConfigClient appConfigClient;
    private ModuleTypeClient moduleTypeClient;
    private TaskExecutor taskExecutor;
    private UrlFetcher urlFetcher;
    private String userId;
    private Model model;

    @Override
    protected void setUp() {
        planAgent = new PlanAgent();
        kiwiAgent = new KiwiAgent(new MockCompiler());
        webAgent = new WebAgent(new MockPageCompiler());
        dataAgent = new MockDataAgent();
        testTaskFactory = new MockTestTaskFactory();
        exchangeClient = new MockExchangeClient();
        planConfigClient = new MockPlanConfigClient();
        moduleTypeClient = new MockModuleTypeClient();
        appConfigClient = new MockAppConfigClient(planConfigClient, moduleTypeClient);
        userClient = new MockUserClient(appConfigClient);
        appClient = new MockAppClient(userClient, appConfigClient);
        userId = userClient.register(new RegisterRequest("kiwi", "123456"));
        taskExecutor = new SyncTaskExecutor();
        model = new MockModel();
        urlFetcher = new UrlFetcher(Constants.CHAT_HOST) {

            @Override
            public UrlResource fetch(String urlOrPath) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void testGeneration() {
        var genService = createGenerationService();
        var prompt = """
                class Foo {}
                """;
        genService.generate(GenerationRequest.create(null, prompt),userId, discardListener);
        var appId = exchangeClient.getLast().getAppId();
        var app = appClient.get(appId);
        var kiwiAppId = app.getKiwiAppId();
//        assertEquals("Test App", app.getName());

        assertEquals("class Foo {}\n", kiwiAgent.getCode(kiwiAppId + "", MAIN_KIWI));
        assertEquals("// Test App\nclass Foo {}\n", webAgent.getCode(kiwiAppId + "", APP_TSX));
        assertEquals(
        String.format("""
                Status: SUCCESSFUL
                Product URL: https://%d.metavm.test
                    Task Kiwi: SUCCESSFUL
                        Attempt SUCCESSFUL
                    Task Web: SUCCESSFUL
                        Attempt SUCCESSFUL
                """, kiwiAppId),
                exchangeClient.getLast().toString()
        );

        genService.generate(GenerationRequest.create(appId, "class Bar{}"), userId, discardListener);
        assertEquals("class Bar{}\n", kiwiAgent.getCode(kiwiAppId + "", MAIN_KIWI));
        assertEquals("class Bar{}\n", webAgent.getCode(kiwiAppId + "", APP_TSX));
        assertEquals(
                String.format("""
                        Status: SUCCESSFUL
                        Product URL: https://%d.metavm.test
                            Task Kiwi: SUCCESSFUL
                                Attempt SUCCESSFUL
                            Task Web: SUCCESSFUL
                                Attempt SUCCESSFUL
                            Task Web: SUCCESSFUL
                                Attempt SUCCESSFUL
                        """, kiwiAppId),
                exchangeClient.getLast().toString()
        );


        genService.generate(GenerationRequest.create(appId, "class Error{}"), userId, discardListener);
        assertEquals("class Fixed{}\n", kiwiAgent.getCode(kiwiAppId + "", MAIN_KIWI));
        assertEquals("class Fixed{}\n", webAgent.getCode(kiwiAppId + "", APP_TSX));
        assertEquals(
                String.format("""
                        Status: SUCCESSFUL
                        Product URL: https://%d.metavm.test
                            Task Kiwi: SUCCESSFUL
                                Attempt FAILED
                                    Compilation failed.
                                Attempt SUCCESSFUL
                            Task Web: SUCCESSFUL
                                Attempt FAILED
                                    Compilation failed.
                                Attempt SUCCESSFUL
                            Task Web: SUCCESSFUL
                                Attempt SUCCESSFUL
                        """, kiwiAppId),
                exchangeClient.getLast().toString()
        );

    }

    public void testCancel() {
        taskExecutor = new DelayedTaskExecutor();
        var generationService = createGenerationService();
        generationService.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
        var exch = exchangeClient.getFirst();
        generationService.cancel(new CancelRequest(exch.getId()));
        try {
            ((DelayedTaskExecutor) taskExecutor).flush();
            fail("Cancelled task should throw an exception");
        } catch (BusinessException e) {
            assertSame(ErrorCode.TASK_CANCELLED, e.getErrorCode());
        }
        var exch1 = exchangeClient.getFirst();
        assertSame(ExchangeStatus.CANCELLED, exch1.getStatus());
    }

    public void testHideAttempts() {
        var genService = createGenerationService();
        var listener = new ProgressListener();
        genService.generate(GenerationRequest.create(null, "class Foo {}"), userId, listener);
    }

    public void testRetry() {
        var failing = new Object() {
            boolean value = true;
        };
        kiwiAgent = new KiwiAgent(new MockCompiler() {
            @Override
            public DeployResult run(long appId, String projectName, List<SourceFile> sourceFiles, List<Path> removedFiles, boolean deploySource, boolean noBackup) {
                if (failing.value)
                    throw new RuntimeException("Failed");
                return super.run(appId, projectName, sourceFiles, removedFiles, deploySource, noBackup);
            }
        });
        var generationService = createGenerationService();
        try {
            generationService.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
            fail();
        }
        catch (Exception ignored) {}
        var exch = exchangeClient.getFirst();
        assertSame(ExchangeStatus.FAILED, exch.getStatus());
        failing.value = false;
        generationService.retry(userId, new RetryRequest(exch.getId()), new ProgressListener());
        var exch1 = exchangeClient.getFirst();
        assertSame(ExchangeStatus.SUCCESSFUL, exch1.getStatus());
    }

    public void testRetryTest() {
        var ref = new Object() {
            boolean retrying;
        };
        planAgent = new PlanAgent() {

            int runs;

            @Override
            public Plan plan(PlanRequest request) {
                if (runs++ == 0)
                    return super.plan(request);
                else
                    return new Plan(null, List.of(
                            new Plan.TestTask("Web")
                    ));
            }
        };
        kiwiAgent = new KiwiAgent(new MockCompiler() {

            @Override
            public DeployResult run(long appId, String projectName, List<SourceFile> sourceFiles, List<Path> removedFiles, boolean deploySource, boolean noBackup) {
                log.debug("Generating");
                if (ref.retrying)
                    throw new RuntimeException("Should not regenerate");
                return super.run(appId, projectName, sourceFiles, removedFiles, deploySource, noBackup);
            }
        });
        testTaskFactory = new MockTestTaskFactory() {

            @Override
            public TestTask createTestTask(long appId, String projectName, String url, Model model, String promptTemplate, String requirement, ModuleRT module, CodeAgentListener listener, AbortController abortController, Consumer<String> setTargetId) {
                return new MockTestTask(listener) {

                    @Override
                    public TestResult runTest() {
                        setTargetId.accept("1");
                        if (ref.retrying)
                            return TestResult.ACCEPTED;
                        else
                            throw new AgentException("Mock failure");
                    }
                };
            }

        };
        var service = createGenerationService();
        var appId = service.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
        try {
            service.generate(GenerationRequest.create(appId, "class Foo{}"), userId, discardListener);
            fail("Should have failed");
        } catch (AgentException ignored) {
        }
        var exch = exchangeClient.getLast();
        log.debug("\n{}", exch.toString());
        assertEquals(ExchangeStatus.FAILED, exch.getStatus());
        for (ExchangeTask task : exch.getTasks()) {
            log.debug("Task {}: {}", task.getModuleName(), task.getStatus());
        }
        ref.retrying = true;
        service.retry(userId, new RetryRequest(exch.getId()), discardListener);
        var exch1 = exchangeClient.getFirst();
        assertEquals(ExchangeStatus.SUCCESSFUL, exch1.getStatus());
    }

    public void testFix() {
        var ref = new Object() {
            int runs;
        };
        testTaskFactory = new MockTestTaskFactory() {

            @Override
            public TestTask createTestTask(long appId, String projectName, String url, Model model, String promptTemplate, String requirement, ModuleRT module, CodeAgentListener listener, AbortController abortController, Consumer<String> setTargetId) {
                return new MockTestTask(listener) {

                    @Override
                    public TestResult runTest() {
                        return ref.runs++ == 0 ?
                                TestResult.reject("reject", new byte[0], "", "", "web")
                                : TestResult.ACCEPTED;
                    }
                };
            }
        };
        var service = createGenerationService();
        var appId = service.generate(GenerationRequest.create(null, "class Foo {}"), userId, discardListener);
        service.generate(GenerationRequest.create(appId, "class Foo {}"), userId, discardListener);
        assertEquals(3, ref.runs);
    }

    public void testPreventingDuplicateGeneration() {
        taskExecutor = t -> {};
        var genService = createGenerationService();
        var appId = genService.generate(GenerationRequest.create(null, "class Foo {}"), userId, discardListener);
        try {
            genService.generate(GenerationRequest.create(appId, "class Foo {}"), userId, discardListener);
            fail("Should not allow duplicate generation");
        } catch (BusinessException e) {
            assertSame(ErrorCode.GENERATION_ALREADY_RUNNING, e.getErrorCode());
        }
    }

    public void testFailExpiredExchanges() {
        taskExecutor = t -> {};
        var genService = createGenerationService();
        genService.generate(GenerationRequest.create(null, "class Foo {}"), userId, discardListener);
        var exch = exchangeClient.getFirst();
        assertTrue(exch.isRunning());
        exch.setLastHeartBeatAt(System.currentTimeMillis() - 1000 * 60 * 10); // Set last heartbeat to 10 minutes ago
        genService.failExpiredExchanges();
        exch = exchangeClient.get(exch.getId());
        assertSame(ExchangeStatus.FAILED, exch.getStatus());
        assertEquals("Timeout", exch.getErrorMessage());
    }

    public void testReconnect() {
        var delayedTaskExecutor = new DelayedTaskExecutor();
        taskExecutor = delayedTaskExecutor;
        var genService = createGenerationService();
        genService.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
        var exchId = exchangeClient.getFirst().getId();
        var listener = new ProgressListener();
        genService.reconnect(exchId, listener);
        delayedTaskExecutor.flush();
        assertFalse(listener.exchanges.isEmpty());
    }

    public void testReconnectToLostTask() {
        taskExecutor = t -> {};
        var genService = createGenerationService();
        genService.generate(GenerationRequest.create(null, "class Foo {}"), userId, discardListener);
        var exch = exchangeClient.getFirst();
        genService.discardTask(exch.getId());
        assertTrue(exch.isRunning());
        try {
            genService.reconnect(exch.getId(), discardListener);
            fail("Should not allow reconnecting to a lost task");
        } catch (BusinessException e) {
            assertSame(ErrorCode.TASK_NOT_RUNNING, e.getErrorCode());
        }
        genService.cancel(new CancelRequest(exch.getId()));
        assertSame(ExchangeStatus.CANCELLED, exchangeClient.get(exch.getId()).getStatus());
    }

    private GenerationService createGenerationService() {
        return new GenerationService(List.of(model),
                planAgent,
                List.of(kiwiAgent, webAgent),
                dataAgent,
                List.of(testTaskFactory),
                exchangeClient,
                appClient,
                appConfigClient,
                userClient,
                moduleTypeClient,
                planConfigClient,
                "https://{}.metavm.test",
                "https://metavm.test/{}", "https://admin.metavm.test/source-{}.zip",
                urlFetcher,
                taskExecutor);

    }

    public void testRevert() {
        var generationService = createGenerationService();
        var appId = generationService.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
        var exch = exchangeClient.getFirst();
        var app = appClient.get(appId);
        assertSame(ExchangeStatus.SUCCESSFUL, exch.getStatus());
        generationService.revert(exch.getId());
        var exch1 = exchangeClient.getFirst();
        assertSame(ExchangeStatus.REVERTED, exch1.getStatus());
        assertEquals(0, kiwiAgent.getSourceFiles(app.getKiwiAppId() + "").size());
        assertEquals(1, webAgent.getSourceFiles(app.getKiwiAppId() + "").size());
    }

    public void testListener() {
        var generationService = createGenerationService();
        var exchanges = new ArrayList<ExchangeDTO>();
        generationService.generate(GenerationRequest.create(null, "class Foo{}"), userId, new GenerationListener() {
            @Override
            public void onThought(String thoughtChunk) {

            }

            @Override
            public void onContent(String contentChunk) {

            }

            @Override
            public void onProgress(ExchangeDTO exchange) {
                exchanges.add(exchange);
            }
        });
        log.debug("# Exchanges: {}", exchanges.size());
    }

    public void testReject() {
        testTaskFactory = new MockTestTaskFactory() {

            @Override
            public TestTask createTestTask(long appId, String projectName, String url, Model model, String promptTemplate, String requirement, ModuleRT module, CodeAgentListener listener, AbortController abortController, Consumer<String> setTargetId) {
                return new MockTestTask(listener) {
                    int trials;

                    @Override
                    public TestResult runTest() {
                        return trials++ == 0 ? TestResult.reject("reject", new byte[0], "", "", "WEB") : TestResult.ACCEPTED;
                    }

                };
            }

        };
        var generationService = createGenerationService();
        var appId = generationService.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
        generationService.generate(GenerationRequest.create(appId, "class Foo{}"), userId, discardListener);
        var app = appClient.get(appId);
        var projName = app.getKiwiAppId() + "";
        var code = webAgent.getCode(projName, "src/App.tsx");
        assertEquals("""
                Fix the following issue
                **Module: ** WEB
                **Description: **reject
                """, code);
    }

    public void testTesOnly() {
        var ref = new Object() {
            int trials;
        };
        planAgent = new PlanAgent() {

            @Override
            public Plan plan(PlanRequest request) {
                return ref.trials++ == 0 ? super.plan(request) : new Plan(null, List.of(
                        new Plan.ModifyModuleTask("Kiwi", "update")
                ));
            }
        };
        var service = createGenerationService();
        var appId = service.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
        service.generate(GenerationRequest.create(appId, "class Foo{}"), userId, discardListener);
    }

    public void testCreateModule() {
        planAgent = new PlanAgent() {

            int trails;

            @Override
            public Plan plan(PlanRequest request) {
                if (trails++ == 0)
                    return super.plan(request);
                else {
                    return new Plan(null, List.of(new Plan.CreateModuleTask(
                            "admin-web",
                            "admin",
                            List.of("Kiwi"),
                            Tech.WEB,
                            "create"
                    )));
                }
            }
        };
        var service = createGenerationService();
        var appId = service.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
        service.generate(GenerationRequest.create(appId, "class Bar{}"), userId, discardListener);
        var app = appClient.get(appId);
        assertEquals(3, app.getModules().size());
        var newMod = app.getModules().getLast();
        assertEquals("admin-web", newMod.getName());
        assertEquals(List.of(app.getModules().getFirst().getId()), newMod.getDependencyIds());
        var projName = app.getKiwiAppId() + "-admin-web";
        var pkgJson = webAgent.getCode(projName, "package.json");
        assertEquals("""
                {
                    "version": "1.0
                }
                """, pkgJson);
        var apiCode = webAgent.getCode(projName, "src/api.ts");
        assertEquals("api", apiCode);
        var appCode = webAgent.getCode(projName, "src/App.tsx");
        System.out.println(appCode);
        assertEquals("""
                class Bar{}
                """, appCode);
    }

    public void testSwitchBrowserTab() {
        var ref = new Object() {

            int nextId;
            int runs;

        };
        testTaskFactory = new MockTestTaskFactory() {

            @Override
            public TestTask createTestTask(long appId, String projectName, String url, Model model, String promptTemplate, String requirement, ModuleRT module, CodeAgentListener listener, AbortController abortController, Consumer<String> setTargetId) {
                return new MockTestTask(listener) {

                    final String pageId = Integer.toString(ref.nextId++);

                    @Override
                    public TestResult runTest() {
                        setTargetId.accept(pageId);
                        return ref.runs++ == 0 ? reject("Web") : TestResult.ACCEPTED;
                    }
                };
            }
        };

        var service = createGenerationService();
        var targetIds = new ArrayList<String>();
        var appId = service.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
        service.generate(GenerationRequest.create(appId, "class Foo{}"), userId, new ProgressListener() {

            @Override
            public void onProgress(ExchangeDTO exchange) {
                if (exchange.testPageId() != null && (targetIds.isEmpty() || !targetIds.getLast().equals(exchange.testPageId()))) {
                    targetIds.add(exchange.testPageId());
                }
            }
        });
        assertEquals(2, ref.nextId);
        assertEquals(3, ref.runs);
        assertEquals(List.of("0", "1", "0"), targetIds);
    }

    public void testMultiModuleTest() {
        var ref = new Object() {

            int testRuns;

        };
        planAgent = new PlanAgent() {

            int runs;

            @Override
            public Plan plan(PlanRequest request) {
                if (runs++ == 0)
                    return super.plan(request);
                else {
                    return new Plan(null, List.of(
                            new Plan.ModifyModuleTask("Web", "modify"),
                            new Plan.ModifyModuleTask("Admin-Web", "modify"),
                            new Plan.TestTask("Web"),
                            new Plan.TestTask("Admin-Web")

                    ));
                }
            }
        };
        testTaskFactory = new MockTestTaskFactory() {

            @Override
            public TestTask createTestTask(long appId, String projectName, String url, Model model, String promptTemplate, String requirement, ModuleRT module, CodeAgentListener listener, AbortController abortController, Consumer<String> setTargetId) {
                return new MockTestTask(listener) {

                    @Override
                    public TestResult runTest() {
                        ref.testRuns++;
                        return super.runTest();
                    }
                };
            }
        };
        var service = createGenerationService();
        var appId = service.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
        var app = appClient.get(appId);
        app.addModule(new Module(null, "Admin-Web", app.getKiwiAppId() + "-admin-web",
                Tech.WEB, "admin web",
                app.getModules().getLast().getTypeId(),
                List.of(
                        app.getModules().getFirst().getId()
                )
        ));
        appClient.save(app);
        ref.testRuns = 0;
        service.generate(GenerationRequest.create(appId, "class Foo{}"), userId, discardListener);
        assertEquals(2, ref.testRuns);
    }

    public void testDataTask() {
        var ref = new Object() {
          int dataTaskRuns;
        };
        dataAgent = new MockDataAgent() {

            @Override
            public void run(DataManipulationRequest request) {
                ref.dataTaskRuns++;
            }
        };
        planAgent = new PlanAgent() {

            @Override
            public Plan plan(PlanRequest request) {
                var plan = super.plan(request);
                if (plan.appName() != null)
                    return plan;
                return new Plan(null, List.of(
                        new Plan.DataTask("Kiwi", "Manipulate data")
                ));
            }
        };
        var service = createGenerationService();
        var appId = service.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
        service.generate(GenerationRequest.create(appId, "class Foo{}"), userId, discardListener);
        assertEquals(1, ref.dataTaskRuns);
        var exch = exchangeClient.getLast();
        assertSame(ExchangeStatus.SUCCESSFUL, exch.getStatus());
        assertEquals(1, exch.getTasks().size());
        var task = exch.getTasks().getLast();
        assertSame(ExchangeTaskType.DATA, task.getType());
        assertSame(ExchangeTaskStatus.SUCCESSFUL, task.getStatus());
    }

    private TestResult reject(String moduleName) {
        return TestResult.reject("reject", new byte[0], "", "", moduleName);
    }

    private final GenerationListener discardListener = new GenerationListener() {
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

    private static class DelayedTaskExecutor implements TaskExecutor {

        private final List<Runnable> pendingTasks = new ArrayList<>();

        @Override
        public void execute(@Nonnull Runnable task) {
            pendingTasks.add(task);
        }

        void flush() {
            pendingTasks.forEach(Runnable::run);
        }

    }

    private static class ProgressListener implements GenerationListener {

        final List<ExchangeDTO> exchanges = new ArrayList<>();

        @Override
        public void onThought(String thoughtChunk) {

        }

        @Override
        public void onContent(String contentChunk) {

        }

        @Override
        public void onProgress(ExchangeDTO exchange) {
            exchanges.add(exchange);
        }
    }

}