package org.kiwi.console.generate;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.file.UrlFetcher;
import org.kiwi.console.file.UrlResource;
import org.kiwi.console.generate.event.GenerationListener;
import org.kiwi.console.generate.rest.CancelRequest;
import org.kiwi.console.generate.rest.GenerationRequest;
import org.kiwi.console.generate.rest.RetryRequest;
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

import static org.kiwi.console.util.Constants.APP_TSX;
import static org.kiwi.console.util.Constants.MAIN_KIWI;

@Slf4j
public class GenerationServiceTest extends TestCase {

    private MockCompiler kiwiCompiler;
    private MockPageCompiler pageCompiler;
    private AppClient appClient;
    private MockExchangeClient exchangeClient;
    private UserClient userClient;
    private MockGenerationConfigClient genConfigClient;
    private AttachmentService attachmentService;
    private UrlFetcher urlFetcher;
    private String userId;

    @Override
    protected void setUp() {
        kiwiCompiler = new MockCompiler();
        pageCompiler = new MockPageCompiler();
        exchangeClient = new MockExchangeClient();
        genConfigClient = new MockGenerationConfigClient();
        userClient = new MockUserClient(genConfigClient);
        appClient = new MockAppClient(userClient);
        attachmentService = new MockAttachmentService();
        userId = userClient.register(new RegisterRequest("kiwi", "123456"));
        urlFetcher = new UrlFetcher(Constants.CHAT_HOST) {

            @Override
            public UrlResource fetch(String urlOrPath) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void testGeneration() {
        var genService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                userClient, "http://{}.metavm.test",
                "http://localhost:8080",
                genConfigClient,
                urlFetcher,
                new SyncTaskExecutor());
        var prompt = """
                class Foo {}
                """;
        genService.generate(GenerationRequest.create(null, prompt),userId, discardListener);
        var appId = exchangeClient.getLast().getAppId();
        var app = appClient.get(appId);
        var sysAppId = app.getKiwiAppId();
//        assertEquals("Test App", app.getName());

        assertEquals("class Foo {}\n", kiwiCompiler.getCode(sysAppId, MAIN_KIWI));
        assertEquals("// Test App\nclass Foo {}\n", pageCompiler.getCode(sysAppId, APP_TSX));
        assertEquals(
        String.format("""
                Status: SUCCESSFUL
                Product URL: http://%d.metavm.test
                    Stage BACKEND: SUCCESSFUL
                        Attempt SUCCESSFUL
                    Stage FRONTEND: SUCCESSFUL
                        Attempt SUCCESSFUL
                """, sysAppId),
                exchangeClient.getLast().toString()
        );

        genService.generate(GenerationRequest.create(appId, "class Bar{}"), userId, discardListener);
        assertEquals("class Bar{}\n", kiwiCompiler.getCode(sysAppId, MAIN_KIWI));
        assertEquals("class Bar{}\n", pageCompiler.getCode(sysAppId, APP_TSX));
        assertEquals(
                String.format("""
                        Status: SUCCESSFUL
                        Product URL: http://%d.metavm.test
                            Stage BACKEND: SUCCESSFUL
                                Attempt SUCCESSFUL
                            Stage FRONTEND: SUCCESSFUL
                                Attempt SUCCESSFUL
                        """, sysAppId),
                exchangeClient.getLast().toString()
        );


        genService.generate(GenerationRequest.create(appId, "class Error{}"), userId, discardListener);
        assertEquals("class Fixed{}\n", kiwiCompiler.getCode(sysAppId, MAIN_KIWI));
        assertEquals("class Fixed{}\n", pageCompiler.getCode(sysAppId, APP_TSX));
        assertEquals(
                String.format("""
                        Status: SUCCESSFUL
                        Product URL: http://%d.metavm.test
                            Stage BACKEND: SUCCESSFUL
                                Attempt FAILED
                                    Compilation failed.
                                Attempt SUCCESSFUL
                            Stage FRONTEND: SUCCESSFUL
                                Attempt FAILED
                                    Compilation failed.
                                Attempt SUCCESSFUL
                        """, sysAppId),
                exchangeClient.getLast().toString()
        );

    }

    public void testCancel() {
        var taskExecutor = new DelayedTaskExecutor();
        var generationService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                userClient, "http://{}.metavm.test",
                "http://localhost:8080",
                genConfigClient,
                urlFetcher,
                taskExecutor);
        generationService.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
        var exch = exchangeClient.getFirst();
        generationService.cancel(new CancelRequest(exch.getId()));
        try {
            taskExecutor.flush();
            fail("Cancelled task should throw an exception");
        } catch (BusinessException e) {
            assertSame(ErrorCode.TASK_CANCELLED, e.getErrorCode());
        }
        var exch1 = exchangeClient.getFirst();
        assertSame(ExchangeStatus.CANCELLED, exch1.getStatus());
    }

    public void testHideAttempts() {
        var genService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                userClient,
                "http://{}.metavm.test",
                "http://localhost:8080",
                genConfigClient,
                urlFetcher,
                new SyncTaskExecutor());
        var exchanges = new ArrayList<Exchange>();
        genService.generate(GenerationRequest.create(null, "class Foo {}"), userId, new GenerationListener() {
            @Override
            public void onThought(String thoughtChunk) {

            }

            @Override
            public void onContent(String contentChunk) {

            }

            @Override
            public void onProgress(Exchange exchange) {
                exchanges.add(exchange);
            }
        });
        for (Exchange exchange : exchanges) {
            for (Stage stage : exchange.getStages()) {
                assertEquals(0, stage.getAttempts().size());
            }
        }
    }

    public void testRetry() {
        var kiwiCompiler = new MockCompiler() {

            boolean failing = true;

            @Override
            public DeployResult run(long appId, List<SourceFile> sourceFiles, List<Path> removedFiles) {
                if (failing)
                    throw new RuntimeException("Failed");
                return super.run(appId, sourceFiles, removedFiles);
            }
        };
        var generationService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                userClient, "http://{}.metavm.test",
                "http://localhost:8080",
                genConfigClient,
                urlFetcher,
                new SyncTaskExecutor());
        try {
            generationService.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
            fail();
        }
        catch (Exception ignored) {}
        var exch = exchangeClient.getFirst();
        assertSame(ExchangeStatus.FAILED, exch.getStatus());
        kiwiCompiler.failing = false;
        generationService.retry(userId, new RetryRequest(exch.getId()), discardListener);
        var exch1 = exchangeClient.getFirst();
        assertSame(ExchangeStatus.SUCCESSFUL, exch1.getStatus());
    }

    public void testPreventingDuplicateGeneration() {
        var genService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                userClient, "http://{}.metavm.test",
                "http://localhost:8080",
                genConfigClient,
                urlFetcher,
                t -> {});
        var appId = genService.generate(GenerationRequest.create(null, "class Foo {}"), userId, discardListener);
        try {
            genService.generate(GenerationRequest.create(appId, "class Foo {}"), userId, discardListener);
            fail("Should not allow duplicate generation");
        } catch (BusinessException e) {
            assertSame(ErrorCode.GENERATION_ALREADY_RUNNING, e.getErrorCode());
        }
    }

    public void testFailExpiredExchanges() {
        var genService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                userClient, "http://{}.metavm.test",
                "http://localhost:8080",
                genConfigClient,
                urlFetcher,
                t -> {});
        genService.generate(GenerationRequest.create(null, "class Foo {}"), userId, discardListener);
        var exch = exchangeClient.getFirst();
        assertTrue(exch.isRunning());
        exch.setLastHeartBeatAt(System.currentTimeMillis() - 1000 * 60 * 10); // Set last heartbeat to 10 minutes ago
        genService.failExpiredExchanges();
        exch = exchangeClient.get(exch.getId());
        assertSame(ExchangeStatus.FAILED, exch.getStatus());
        assertEquals("Timeout", exch.getErrorMessage());
    }

    public void testReconnectToLostTask() {
        var genService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                userClient, "http://{}.metavm.test",
                "http://localhost:8080",
                genConfigClient,
                urlFetcher,
                t -> {});
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

    public void testRevert() {
        var generationService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                userClient, "http://{}.metavm.test",
                "http://localhost:8080",
                genConfigClient,
                urlFetcher,
                new SyncTaskExecutor());
        var appId = generationService.generate(GenerationRequest.create(null, "class Foo{}"), userId, discardListener);
        var exch = exchangeClient.getFirst();
        var app = appClient.get(appId);
        assertSame(ExchangeStatus.SUCCESSFUL, exch.getStatus());
        generationService.revert(exch.getId());
        var exch1 = exchangeClient.getFirst();
        assertSame(ExchangeStatus.REVERTED, exch1.getStatus());
        assertEquals(0, kiwiCompiler.getSourceFiles(app.getKiwiAppId()).size());
        assertEquals(1, pageCompiler.getSourceFiles(app.getKiwiAppId()).size());
    }

    private final GenerationListener discardListener = new GenerationListener() {
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

}