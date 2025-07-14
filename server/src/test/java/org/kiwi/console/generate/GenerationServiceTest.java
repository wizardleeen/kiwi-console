package org.kiwi.console.generate;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.generate.event.GenerationListener;
import org.kiwi.console.generate.rest.CancelRequest;
import org.kiwi.console.generate.rest.RetryRequest;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.util.BusinessException;
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

    public void testGeneration() {
        var kiwiCompiler = new MockCompiler();
        var pageCompiler = new MockCompiler();
        var appClient = new MockAppClient();
        var exchangeClient = new MockExChangeClient();
        var genService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                new MockUserClient(), "http://{}.metavm.test",
                "http://localhost:8080",
                new SyncTaskExecutor()
        );
        var prompt = """
                class Foo {}
                """;
        genService.generate(null, prompt, "tk", false, discardListener);
        var appId = exchangeClient.getLast().getAppId();
        var app = appClient.get(appId);
        var sysAppId = app.getSystemAppId();
//        assertEquals("Test App", app.getName());

        assertEquals("class Foo {}\n", kiwiCompiler.getCode(sysAppId, MAIN_KIWI));
        assertEquals("class Foo {}\n", pageCompiler.getCode(sysAppId, APP_TSX));
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

        genService.generate(appId, "class Bar{}", "tk", false, discardListener);
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


        genService.generate(appId, "class Error{}", "tk", false, discardListener);
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
        var kiwiCompiler = new MockCompiler();
        var pageCompiler = new MockCompiler();
        var taskExecutor = new DelayedTaskExecutor();
        var exchClient = new MockExChangeClient();
        var generationService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchClient,
                new MockAppClient(),
                new MockUserClient(), "http://{}.metavm.test",
                "http://localhost:8080",
                taskExecutor
        );
        generationService.generate(null, "class Foo{}", "001", false, discardListener);
        var exch = exchClient.getFirst();
        generationService.cancel(new CancelRequest(exch.getId()));
        try {
            taskExecutor.flush();
            fail("Cancelled task should throw an exception");
        } catch (BusinessException e) {
            assertSame(ErrorCode.TASK_CANCELLED, e.getErrorCode());
        }
        var exch1 = exchClient.getFirst();
        assertSame(ExchangeStatus.CANCELLED, exch1.getStatus());
    }

    public void testHideAttempts() {
        var kiwiCompiler = new MockCompiler();
        var pageCompiler = new MockCompiler();
        var appClient = new MockAppClient();
        var exchangeClient = new MockExChangeClient();
        var userService = new MockUserClient() {

            @Override
            public boolean shouldShowAttempts(UserIdRequest request) {
                return false;
            }
        };

        var genService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                userService,
                "http://{}.metavm.test",
                "http://localhost:8080",
                new SyncTaskExecutor()
        );
        var exchanges = new ArrayList<Exchange>();
        genService.generate(null, "class Foo {}", "", false, new GenerationListener() {
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
        var pageCompiler = new MockCompiler();
        var exchClient = new MockExChangeClient();
        var userClient = new MockUserClient();
        var userId = userClient.register(new RegisterRequest("leen", "123456"));
        var generationService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchClient,
                new MockAppClient(),
                userClient, "http://{}.metavm.test",
                "http://localhost:8080",
                new SyncTaskExecutor()
        );
        try {
            generationService.generate(null, "class Foo{}", "001", false, discardListener);
            fail();
        }
        catch (Exception ignored) {}
        var exch = exchClient.getFirst();
        assertSame(ExchangeStatus.FAILED, exch.getStatus());
        kiwiCompiler.failing = false;
        generationService.retry(userId, new RetryRequest(exch.getId()), discardListener);
        var exch1 = exchClient.getFirst();
        assertSame(ExchangeStatus.SUCCESSFUL, exch1.getStatus());
    }

    public void testPreventingDuplicateGeneration() {
        var kiwiCompiler = new MockCompiler();
        var pageCompiler = new MockCompiler();
        var appClient = new MockAppClient();
        var exchangeClient = new MockExChangeClient();
        var genService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                new MockUserClient(), "http://{}.metavm.test",
                "http://localhost:8080",
                t -> {}
        );
        var appId = genService.generate(null, "class Foo {}", "001", false, discardListener);
        try {
            genService.generate(appId, "class Foo {}", "001", false, discardListener);
            fail("Should not allow duplicate generation");
        } catch (BusinessException e) {
            assertSame(ErrorCode.GENERATION_ALREADY_RUNNING, e.getErrorCode());
        }
    }

    public void testFailExpiredExchanges() {
        var kiwiCompiler = new MockCompiler();
        var pageCompiler = new MockCompiler();
        var appClient = new MockAppClient();
        var exchangeClient = new MockExChangeClient();
        var genService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                new MockUserClient(), "http://{}.metavm.test",
                "http://localhost:8080",
                t -> {}
        );
        genService.generate(null, "class Foo {}", "001", false, discardListener);
        var exch = exchangeClient.getFirst();
        assertTrue(exch.isRunning());
        exch.setLastHeartBeatAt(System.currentTimeMillis() - 1000 * 60 * 10); // Set last heartbeat to 10 minutes ago
        genService.failExpiredExchanges();
        exch = exchangeClient.get(exch.getId());
        assertSame(ExchangeStatus.FAILED, exch.getStatus());
        assertEquals("Timeout", exch.getErrorMessage());
    }

    public void testReconnectToLostTask() {
        var kiwiCompiler = new MockCompiler();
        var pageCompiler = new MockCompiler();
        var appClient = new MockAppClient();
        var exchangeClient = new MockExChangeClient();
        var genService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                new MockUserClient(), "http://{}.metavm.test",
                "http://localhost:8080",
                t -> {}
        );
        genService.generate(null, "class Foo {}", "001", false, discardListener);
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