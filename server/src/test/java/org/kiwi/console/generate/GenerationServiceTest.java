package org.kiwi.console.generate;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.generate.event.GenerationListener;
import org.kiwi.console.generate.rest.CancelRequest;
import org.kiwi.console.generate.rest.RetryRequest;
import org.kiwi.console.kiwi.Exchange;
import org.kiwi.console.kiwi.ExchangeStatus;
import org.kiwi.console.kiwi.MockApplicationClient;
import org.kiwi.console.kiwi.MockExChangeClient;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static org.kiwi.console.util.Constants.APP_TSX;
import static org.kiwi.console.util.Constants.MAIN_KIWI;

@Slf4j
public class GenerationServiceTest extends TestCase {

    public void testChat() {
        var kiwiCompiler = new MockCompiler();
        var pageCompiler = new MockCompiler();
        var appClient = new MockApplicationClient();
        var exchangeClient = new MockExChangeClient();
        var chatService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchangeClient,
                appClient,
                "http://{}.metavm.test",
                "http://localhost:8080",
                "",
                new SyncTaskExecutor()
        );
        var prompt = """
                // Test App
                class Foo {}
                """;
        chatService.generate(null, prompt, "tk", false, discardListener);
        var appId = exchangeClient.getLast().getAppId();
        var app = appClient.get(appId);
        var sysAppId = app.getSystemAppId();
        assertEquals("Test App", app.getName());

        assertEquals(prompt, kiwiCompiler.getCode(sysAppId, MAIN_KIWI));
        assertEquals(prompt, pageCompiler.getCode(sysAppId, APP_TSX));
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

        chatService.generate(appId, "class Bar{}", "tk", false, discardListener);
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


        chatService.generate(appId, "class Error{}", "tk", false, discardListener);
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
                new MockApplicationClient(),
                "http://{}.metavm.test",
                "http://localhost:8080",
                "",
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

    public void testRetry() {
        var kiwiCompiler = new MockCompiler() {

            boolean failing = true;

            @Override
            public DeployResult run(long appId, String token, List<SourceFile> sourceFiles) {
                if (failing)
                    throw new RuntimeException("Failed");
                return super.run(appId, token, sourceFiles);
            }
        };
        var pageCompiler = new MockCompiler();
        var exchClient = new MockExChangeClient();
        var generationService = new GenerationService(new MockAgent(), kiwiCompiler, pageCompiler,
                exchClient,
                new MockApplicationClient(),
                "http://{}.metavm.test",
                "http://localhost:8080",
                "",
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
        generationService.retry(new RetryRequest(exch.getId()), discardListener);
        var exch1 = exchClient.getFirst();
        assertSame(ExchangeStatus.SUCCESSFUL, exch1.getStatus());
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