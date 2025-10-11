package org.kiwi.console.generate;

import org.kiwi.console.browser.Browser;
import org.kiwi.console.kiwi.Tech;

import java.nio.file.Path;
import java.util.function.Consumer;

public class WebTestTaskFactory implements TestTaskFactory {

    private final Browser browser;
    private final PageCompiler pageCompiler;
    private final Path testEnvDir;
    private final Path testLogDir;
    private final boolean logOn;

    public WebTestTaskFactory(Browser browser, PageCompiler pageCompiler, Path testEnvDir, Path testLogDir, boolean logOn) {
        this.browser = browser;
        this.pageCompiler = pageCompiler;
        this.testEnvDir = testEnvDir;
        this.testLogDir = testLogDir;
        this.logOn = logOn;
    }

    @Override
    public TestTask createTestTask(long appId, String projectName, String url, Model model, String promptTemplate, String requirement, ModuleRT module, CodeAgentListener listener, AbortController abortController, Consumer<String> setTargetId) {
        return new WebTestTask(
                browser,
                pageCompiler,
                testEnvDir,
                testLogDir,
                logOn,
                appId,
                projectName,
                listener,
                url,
                model,
                promptTemplate,
                requirement,
                module,
                abortController,
                setTargetId
        );
    }

    @Override
    public Tech getTech() {
        return Tech.WEB;
    }
}
