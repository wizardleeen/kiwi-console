package org.kiwi.console.browser;

import com.google.gson.JsonObject;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.FilePayload;
import com.microsoft.playwright.options.ScreenshotType;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.file.File;
import org.kiwi.console.util.Utils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Slf4j
public class PlaywrightPage implements Page {

    private final BrowserContext context;
    private final com.microsoft.playwright.Page page;
    private final List<String> consoleMessages = new ArrayList<>();
    private @Nullable String targetId;

    public PlaywrightPage(BrowserContext context) {
        this.context = context;
        this.page = context.newPage();
        page.setDefaultTimeout(10_000);
        page.setDefaultNavigationTimeout(10_000);
        page.onConsoleMessage(message -> consoleMessages.add(message.text()));

        page.exposeFunction("fail", args -> {
            page.evaluate(String.format("console.error(\"%s\")", Utils.escapeJavaString(Objects.toString(args[0]))));
            return null;
        });
        page.exposeFunction("done", args -> null);
    }

    @Override
    public String getTargetId() {
        if (targetId == null)
            targetId = getTargetId(page);
        return targetId;
    }

    static String getTargetId(com.microsoft.playwright.Page page) {
        CDPSession cdpSession = page.context().newCDPSession(page);
        try {
            JsonObject result = cdpSession.send("Target.getTargetInfo", new JsonObject());
            return result.get("targetInfo").getAsJsonObject().get("targetId").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get targetId from page", e);
        } finally {
            cdpSession.detach();
        }

    }

    @Override
    public void navigate(String url) {
        page.navigate(url);
    }

    @Override
    public void log(String log) {
        page.evaluate("(msg) => console.log(msg)", log);
    }

    @Override
    public byte[] getScreenshot() {
        return page.screenshot(
                new com.microsoft.playwright.Page.ScreenshotOptions()
                        .setFullPage(true)
                        .setType(ScreenshotType.PNG)
        );
    }

    @Override
    public String getConsoleLogs() {
        return String.join("\n", consoleMessages);
    }

    @Override
    public String getDOM() {
        return page.content();
    }

    @Override
    public void hover(String selector) {
        page.locator(selector).hover();
    }

    @Override
    public void mouseDown() {
        page.mouse().down();
    }

    @Override
    public void mouseUp() {
        page.mouse().up();
    }

    @Override
    public void click(String selector) {
        page.locator(selector).click();
    }

    @Override
    public void fill(String selector, String value) {
        page.locator(selector).fill(value);
    }

    @Override
    public void clear(String selector) {
        page.locator(selector).clear();
    }

    @Override
    public void press(String selector, String key) {
        page.locator(selector).press(key);
    }

    @Override
    public void setInputFile(String selector, File file) {
        page.locator(selector).setInputFiles(new FilePayload(file.name(), file.mimeType(), file.bytes()));
    }

    @Override
    public void dragAndDrop(String selector, String targetSelector) {
        page.locator(selector).dragTo(page.locator(targetSelector));
    }

    public boolean isVisible(String selector) {
        return isVisible(selector, null);
    }

    @Override
    public boolean isHidden(String selector) {
        try {
            assertThat(page.locator(selector)).isHidden();
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    @Override
    public boolean containText(String selector, String text) {
        try {
            assertThat(page.locator(selector)).containsText(text);
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    private boolean isVisible(String selector, Double timeout) {
        try {
            Locator locator = page.locator(selector);
            if (timeout != null)
                assertThat(locator).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(timeout));
            else
                assertThat(locator).isVisible();
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    @Override
    public void close() {
        page.close();
        context.close();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlaywrightPage that = (PlaywrightPage) o;
        return Objects.equals(getTargetId(), that.getTargetId());
    }

    @Override
    public int hashCode() {
        return getTargetId().hashCode();
    }
}