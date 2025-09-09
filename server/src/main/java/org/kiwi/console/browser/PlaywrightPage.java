package org.kiwi.console.browser;

import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.FilePayload;
import com.microsoft.playwright.options.ScreenshotType;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.generate.PlaywrightActions;
import org.kiwi.console.util.Utils;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

        page.onConsoleMessage(message -> {
            String logEntry = message.args().stream()
                    .map(arg -> {
                        try {
                            Object value = arg.evaluate("obj => {" +
                                    "  if (obj instanceof Error) {" +
                                    "    return obj.stack;" +
                                    "  }" +
                                    "  const cache = new Set();" +
                                    "  try {" +
                                    "    return JSON.stringify(obj, (key, value) => {" +
                                    "      if (typeof value === 'object' && value !== null) {" +
                                    "        if (cache.has(value)) {" +
                                    "          return '[Circular]';" +
                                    "        }" +
                                    "        cache.add(value);" +
                                    "      }" +
                                    "      return value;" +
                                    "    });" +
                                    "  } catch (e) {" +
                                    "    return String(obj);" +
                                    "  }" +
                                    "}");
                            return String.valueOf(value);
                        } finally {
                            arg.dispose();
                        }
                    })
                    .collect(Collectors.joining(" "));

            consoleMessages.add(logEntry);
        });

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

    /**
     * Executes a given Playwright action and returns its success status.
     * For 'expect' actions, failure results in a console error in the browser and a 'false' return.
     * For all other actions, an exception will be thrown on failure.
     *
     * @param action The Playwright action to execute.
     * @return {@code true} if the action was successful, {@code false} if an 'expect' check failed.
     */
    public ExecuteResult execute(PlaywrightActions.PlaywrightCommand action) {
        try {
            switch (action) {
                case PlaywrightActions.Click click -> page.locator(click.selector()).click();

                case PlaywrightActions.Fill fill -> page.locator(fill.selector()).fill(fill.value());

                case PlaywrightActions.Press press -> page.locator(press.selector()).press(press.value());

                case PlaywrightActions.ExpectVisible expect -> {
                    // We use try-catch to leverage the powerful auto-waiting of the assertion library
                    // without crashing the execution flow on failure.
                    try {
                        Locator locator = page.locator(expect.selector());
                        // Check if a custom timeout is provided in the options
                        if (expect.options() != null && expect.options().get("timeout") instanceof Number n) {
                            // If a timeout is specified, use it
                            double timeout = n.doubleValue();
                            assertThat(locator).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(timeout));
                        } else {
                            // Otherwise, use the default assertion timeout
                            assertThat(locator).isVisible();
                        }
                    } catch (AssertionError e) {
                        String errorMessage = String.format("ExpectVisible failed for selector '%s'. Reason: %s",
                                Utils.escapeJavaString(expect.selector()), Utils.escapeJavaString(e.getMessage()));
                        return ExecuteResult.failed(errorMessage);
                    }
                }

                case PlaywrightActions.ExpectHidden expect -> {
                    // We use try-catch to leverage the powerful auto-waiting of the assertion library
                    // without crashing the execution flow on failure.
                    try {
                        // Using the default timeout from the assertion library.
                        assertThat(page.locator(expect.selector())).isHidden();
                    } catch (AssertionError e) {
                        String errorMessage = String.format("ExpectHidden failed for selector '%s'. Reason: %s",
                                Utils.escapeJavaString(expect.selector()), Utils.escapeJavaString(e.getMessage()));
                        return ExecuteResult.failed(errorMessage);
                    }
                }

                case PlaywrightActions.ExpectToContainText expect -> {
                    // We use try-catch here to leverage the powerful auto-waiting of the assertion library
                    // without crashing the execution flow on failure.
                    try {
                        assertThat(page.locator(expect.selector())).containsText(expect.value());
                    } catch (AssertionError e) {
                        String errorMessage = String.format("ExpectToContainText failed for selector '%s'. Reason: %s",
                                Utils.escapeJavaString(expect.selector()), Utils.escapeJavaString(e.getMessage()));
                        return ExecuteResult.failed(errorMessage);
                    }
                }

                case PlaywrightActions.UploadFile upload -> {
                    Path filePath = Paths.get("src/test/resources/uploads/" + upload.value());
                    if (!Files.exists(filePath)) {
                        throw new RuntimeException("Static file for upload not found at: " + filePath);
                    }
                    page.locator(upload.selector()).setInputFiles(filePath);
                }

                case PlaywrightActions.UploadGeneratedFile upload -> {
                    switch (upload.fileProperties()) {
                        case PlaywrightActions.ImageFileProperties imgProps -> generateAndUploadImage(upload.selector(), imgProps);
                        case PlaywrightActions.TextFileProperties txtProps -> generateAndUploadTextFile(upload.selector(), txtProps);
                    }
                }

                case PlaywrightActions.DragAndDrop dragAndDrop -> {
                    Locator source = page.locator(dragAndDrop.selector());
                    Locator target = page.locator(dragAndDrop.targetSelector());
                    source.dragTo(target);
                }

                default -> throw new UnsupportedOperationException("Execution logic not implemented for action: " + action.action());
            }
            // If we reach here, the action was successful
            return ExecuteResult.success();
        } catch (PlaywrightException e) {
            return ExecuteResult.failed(e.getMessage());

        } catch (IOException e) {
            // Re-throw IOExceptions from file generation as a fatal error
            throw new RuntimeException("Failed during file generation for upload", e);
        }
    }

    /**
     * Generates a text file in memory and uploads it using Playwright.
     *
     * @param selector The selector for the file input element.
     * @param props The properties of the text file to generate.
     */
    private void generateAndUploadTextFile(String selector, PlaywrightActions.TextFileProperties props) {
        page.locator(selector).setInputFiles(new FilePayload(
                props.fileName(),
                props.mimeType(),
                props.content().getBytes(StandardCharsets.UTF_8)
        ));
    }

    /**
     * Generates a placeholder image in memory and uploads it using Playwright.
     *
     * @param selector The selector for the file input element.
     * @param props The properties of the image to generate.
     * @throws IOException If there's an error writing the image bytes.
     */
    private void generateAndUploadImage(String selector, PlaywrightActions.ImageFileProperties props) throws IOException {
        BufferedImage image = new BufferedImage(props.width(), props.height(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Set background color
        g2d.setColor(parseColor(props.backgroundColor(), Color.LIGHT_GRAY));
        g2d.fillRect(0, 0, props.width(), props.height());

        // Set text color and font
        g2d.setColor(parseColor(props.textColor(), Color.DARK_GRAY));
        g2d.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, props.height() / 10)));

        // Center the text
        if (props.text() != null && !props.text().isBlank()) {
            FontMetrics fm = g2d.getFontMetrics();
            int x = (props.width() - fm.stringWidth(props.text())) / 2;
            int y = (fm.getAscent() + (props.height() - (fm.getAscent() + fm.getDescent())) / 2);
            g2d.drawString(props.text(), x, y);
        }

        g2d.dispose();

        // Convert BufferedImage to byte array
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        // Upload the byte array directly
        page.locator(selector).setInputFiles(new FilePayload(
                props.fileName(),
                props.mimeType(),
                imageBytes
        ));
    }

    /**
     * Helper to parse hex color strings with a fallback default.
     * @param hexColor The hex color string (e.g., "#3498db").
     * @param defaultColor The color to use if parsing fails.
     * @return The parsed Color object.
     */
    private Color parseColor(@Nullable String hexColor, Color defaultColor) {
        if (hexColor == null) {
            return defaultColor;
        }
        try {
            return Color.decode(hexColor);
        } catch (NumberFormatException e) {
            return defaultColor;
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