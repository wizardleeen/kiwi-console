package org.kiwi.console.generate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kiwi.console.StackTraceDeobfuscator;
import org.kiwi.console.browser.Browser;
import org.kiwi.console.browser.Page;
import org.kiwi.console.file.File;
import org.kiwi.console.kiwi.Tech;
import org.kiwi.console.patch.PatchReader;
import org.kiwi.console.util.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class WebTestTask implements TestTask {

    private static final int MAX_AUTO_TEST_ACTIONS = 100;
    private static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final PageCompiler pageCompiler;
    private final Path testEnvDir;
    private final Path testLogDir;
    private final boolean logOn;
    private final long appId;
    private final String projectName;
    private final Model model;
    private final String promptTemplate;
    private final String requirement;
    private final ModuleRT module;
    private final AbortController abortController;
    private final CodeAgentListener listener;
    private final Consumer<String> setTargetId;
    private final String testId;
    private final Page page;
    private int step;
    private final List<Action> actions = new ArrayList<>();

    public WebTestTask(Browser browser,
                       PageCompiler pageCompiler,
                       Path testEnvDir,
                       @javax.annotation.Nullable Path testLogDir,
                       boolean logOn,
                       long appId,
                       String projectName, CodeAgentListener listener,
                       String url,
                       Model model,
                       String promptTemplate,
                       String requirement,
                       ModuleRT module,
                       AbortController abortController,
                       Consumer<String> setTargetId
    ) {
        this.listener = listener;
        if (logOn && testLogDir == null)
            throw new IllegalArgumentException("testLogDir cannot be null when logOn is true");
        this.pageCompiler = pageCompiler;
        this.testEnvDir = testEnvDir;
        this.testLogDir = testLogDir;
        this.logOn = logOn;
        this.appId = appId;
        this.projectName = projectName;
        this.model = model;
        this.promptTemplate = promptTemplate;
        this.requirement = requirement;
        this.module = module;
        this.abortController = abortController;
        this.setTargetId = setTargetId;
        page = browser.createPage(url);
        testId = getTestId();
    }

    @Override
    @SneakyThrows
    public TestResult runTest() {
        listener.onAttemptStart();
        if (step == 0)
            page.navigate("/");
        else {
            page.reload();
            actions.add(new FixAction());
        }
        for (; step < MAX_AUTO_TEST_ACTIONS; step++) {
            setTargetId.accept(page.getTargetId());
            if (abortController.isAborted())
                break;
//            log.debug("Console Logs\n{}", consoleLogs);
//            Files.write(Path.of("/tmp/screenshot.png"), screenshot);
            var consoleLogs = page.getConsoleLogs();
            var sourceMapPath = pageCompiler.getSourceMapPath(projectName);
            if (sourceMapPath != null) {
                consoleLogs = StackTraceDeobfuscator.deobfuscate(
                        Files.readString(sourceMapPath),
                        consoleLogs
                );
            }
            var screenshot = page.getScreenshot();
            var dom = page.getDOM();
            logTestInfo(testId, step, consoleLogs, dom, screenshot);
            var action = nextAction(model, appId, projectName, promptTemplate, requirement, screenshot, dom, consoleLogs, actions, abortController);
            actions.add(action);
            if (action instanceof AcceptAction acceptAction) {
                updateTestAccounts(appId, acceptAction.getUpdatedTestAccounts());
                listener.onAttemptSuccess();
                return TestResult.ACCEPTED;
            }
            if (action instanceof RejectAction rejectAction) {
                updateTestAccounts(appId, rejectAction.getUpdatedTestAccounts());
                listener.onAttemptFailure("Rejected: " + rejectAction.bugReport);
                return TestResult.reject(rejectAction.bugReport, screenshot, dom, consoleLogs, module.name());
            }
            if (action instanceof AbortAction abortAction) {
                listener.onAttemptFailure(abortAction.reason);
                return TestResult.abort(abortAction.reason);
            }
            assert action instanceof StepAction;
            var stepAction = (StepAction) action;
            out:
            {
                for (var cmd : stepAction.getCommands()) {
                    var r = executeCommand(page, cmd);
                    if (!r.successful()) {
                        stepAction.setErrorMessage(Objects.requireNonNull(r.errorMessage()));
                        break out;
                    }
                }
                stepAction.setSuccessful(true);
            }
//            log.debug("Last action:\n{}", Utils.toPrettyJSONString(action));
        }
        listener.onAttemptFailure("Max steps reached");
        return TestResult.abort("Test not finished in " + MAX_AUTO_TEST_ACTIONS + "steps");
    }

    @Override
    public Tech getTech() {
        return Tech.WEB;
    }

    @Override
    public void close() {
        page.close();
    }

    @SneakyThrows
    private Action nextAction(Model model,
                              long appId,
                              String projectName,
                              String promptTemplate,
                              String requirement,
                              byte[] screenshot,
                              String dom,
                              String consoleLogs,
                              List<Action> actions,
                              AbortController abortController) {
        var testAccounts = getTestAccounts(appId);
        var code = PatchReader.buildCode(pageCompiler.getSourceFiles(projectName));
        var prompt = Format.format(
                promptTemplate,
                requirement,
                code,
                testAccounts,
                actions.stream().map(Utils::toPrettyJSONString).collect(Collectors.joining("\n"))
        );
        if (actions.isEmpty())
            log.info("\n{}", prompt);
        var wait = 1000;
        for (var i = 0; i < 5; i++) {
            String actionText = null;
            try {
                actionText = Models.generateContent(
                        model,
                        prompt,
                        List.of(
                                new File(screenshot, "image/png"),
                                new File(dom.getBytes(StandardCharsets.UTF_8), "text/html"),
                                new File(consoleLogs.getBytes(StandardCharsets.UTF_8), "text/plain")
                        ),
                        abortController
                );
                return parseAction(actionText);
            } catch (Exception e) {
                log.warn("Failed to parse action: {}", actionText, e);
                Thread.sleep(wait);
                wait *= 2;
            }
        }
        throw new AgentException("Failed to generate action after 5 attempts");
    }

    @SneakyThrows
    private String getTestAccounts(long appId) {
        var filePath = getTestAccountFilePath(appId);
        if (Files.exists(filePath)) {
            return Files.readString(filePath);
        } else
            return "No test account. Create new accounts if needed.";
    }

    @SneakyThrows
    private void updateTestAccounts(long appId, @Nullable String updatedTestAccount) {
        if (updatedTestAccount == null)
            return;
        var path = getTestAccountFilePath(appId);
        Files.createDirectories(path.getParent());
        Files.writeString(path, updatedTestAccount);
    }

    private Path getTestAccountFilePath(long appId) {
        return testEnvDir.resolve(Long.toString(appId)).resolve("test-accounts.txt");
    }


    private ExecuteResult executeCommand(Page page, Command command) {
        try {
            switch (command) {
                case Click click -> {
                    if (!page.isVisible(click.selector))
                        return new ExecuteResult(false, "Element not visible: " + click.selector);
                    page.click(click.selector);
                    return ExecuteResult.success();
                }
                case Fill fill -> {
                    if (!page.isVisible(fill.selector))
                        return new ExecuteResult(false, "Element not visible: " + fill.selector);
                    page.fill(fill.selector, fill.value);
                    return ExecuteResult.success();
                }
                case Press press -> {
                    if (!page.isVisible(press.selector))
                        return new ExecuteResult(false, "Element not visible: " + press.selector);
                    page.press(press.selector, press.value);
                    return ExecuteResult.success();
                }
                case DragAndDrop dragAndDrop -> {
                    if (!page.isVisible(dragAndDrop.selector))
                        return new ExecuteResult(false, "Element not visible: " + dragAndDrop.selector);
                    if (!page.isVisible(dragAndDrop.targetSelector()))
                        return new ExecuteResult(false, "Element not visible: " + dragAndDrop.targetSelector());
                    page.dragAndDrop(dragAndDrop.selector(), dragAndDrop.targetSelector());
                    return ExecuteResult.success();
                }
                case ExpectVisible expectVisible -> {
                    if (!page.isVisible(expectVisible.selector))
                        return new ExecuteResult(false, "Element not visible: " + expectVisible.selector);
                    return ExecuteResult.success();
                }
                case ExpectHidden expectHidden -> {
                    if (page.isVisible(expectHidden.selector))
                        return new ExecuteResult(false, "Element still visible: " + expectHidden.selector);
                    return ExecuteResult.success();
                }
                case ExpectToContainText expectToContainText -> {
                    if (!page.containText(expectToContainText.selector, expectToContainText.value))
                        return new ExecuteResult(false, "Element does not contain expected text: " + expectToContainText.selector);
                    return ExecuteResult.success();
                }
                case UploadGeneratedFile uploadGeneratedFile -> {
                    if (!page.isVisible(uploadGeneratedFile.selector))
                        return new ExecuteResult(false, "Element not visible: " + uploadGeneratedFile.selector);
                    var file = generateFile(uploadGeneratedFile.fileProperties);
                    page.setInputFile(uploadGeneratedFile.selector, file);
                    return ExecuteResult.success();
                }
                case Clear clear -> {
                    if (!page.isVisible(clear.selector))
                        return new ExecuteResult(false, "Element not visible: " + clear.selector);
                    page.clear(clear.selector);
                    return ExecuteResult.success();
                }
                case Hover hover -> {
                    if (!page.isVisible(hover.selector))
                        return new ExecuteResult(false, "Element not visible: " + hover.selector);
                    page.hover(hover.selector);
                    return ExecuteResult.success();
                }
                case MouseDown ignored -> {
                    page.mouseDown();
                    return ExecuteResult.success();
                }
                case MouseUp ignored -> {
                    page.mouseUp();
                    return ExecuteResult.success();
                }
                case Navigate navigate -> {
                    page.navigate(navigate.url);
                    return ExecuteResult.success();
                }
            }
        } catch (Exception e) {
            return new ExecuteResult(false, e.getMessage());
        }
    }

    private File generateFile(FileProperties props) {
        switch (props) {
            case ImageFileProperties imageProps -> {
                var bytes = generateImage(imageProps);
                return new File(imageProps.fileName(), bytes, imageProps.mimeType());
            }
            case TextFileProperties textProps -> {
                var bytes = textProps.content().getBytes(StandardCharsets.UTF_8);
                return new File(textProps.fileName(), bytes, textProps.mimeType());
            }
            case ExcelFileProperties excelProps -> {
                var bytes = generateExcelFile(excelProps.csvContent());
                return new File(excelProps.fileName(), bytes, excelProps.mimeType());
            }
            default -> throw new IllegalArgumentException("Unsupported file type: " + props.type());
        }
    }


    @SneakyThrows
    private byte[] generateExcelFile(String csvContent) {
        try (var workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");

            String[] rows = csvContent.split("\n");
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i);
                String[] cells = rows[i].split(","); // Simple CSV parsing
                for (int j = 0; j < cells.length; j++) {
                    var cell = row.createCell(j);
                    cell.setCellValue(cells[j]);
                }
            }

            var baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

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

    @SneakyThrows
    private byte[] generateImage(ImageFileProperties props) {
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    public interface Action {
        String getType();
    }

    /**
     * A new abstract base class for terminal actions (ACCEPT and REJECT)
     * that can include updated test account information.
     */
    @Data
    public abstract static class FinalAction implements Action {
        @Nullable
        private String updatedTestAccounts;
    }

    @Data
    public static class StepAction implements Action {
        private String description;
        private List<Command> commands;
        private String reason;
        private String bugReport;
        private boolean successful;
        private String errorMessage;

        @Override
        public String getType() {
            return "STEP";
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class RejectAction extends FinalAction {
        private String bugReport;

        @Override
        public String getType() {
            return "REJECT";
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class AcceptAction extends FinalAction {
        @Override
        public String getType() {
            return "ACCEPT";
        }
    }

    @Data
    public static class AbortAction implements Action {
        private String reason;

        @Override
        public String getType() {
            return "ABORT";
        }
    }

    public static class FixAction implements Action {

        @Override
        public String getType() {
            return "FIX";
        }
    }

    // ------------------- Playwright Action Definitions -------------------

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action", include = JsonTypeInfo.As.EXISTING_PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Click.class, name = "click"),
            @JsonSubTypes.Type(value = Fill.class, name = "fill"),
            @JsonSubTypes.Type(value = Press.class, name = "press"),
            @JsonSubTypes.Type(value = ExpectVisible.class, name = "expectVisible"),
            @JsonSubTypes.Type(value = ExpectHidden.class, name = "expectHidden"),
            @JsonSubTypes.Type(value = ExpectToContainText.class, name = "expectToContainText"),
            @JsonSubTypes.Type(value = UploadGeneratedFile.class, name = "uploadGeneratedFile"),
            @JsonSubTypes.Type(value = DragAndDrop.class, name = "dragAndDrop"),
            @JsonSubTypes.Type(value = Clear.class, name = "clear"),
            @JsonSubTypes.Type(value = Hover.class, name = "hover"),
            @JsonSubTypes.Type(value = MouseUp.class, name = "mouseUp"),
            @JsonSubTypes.Type(value = MouseDown.class, name = "mouseDown"),
            @JsonSubTypes.Type(value = Navigate.class, name = "navigate")
    })
    public sealed interface Command {
        @SuppressWarnings("unused")
        String getAction();
    }

    // --- Concrete Action Implementations as Records ---

    public record Click(String selector) implements Command {
        @Override
        public String getAction() {
            return "click";
        }
    }

    public record Fill(String selector, String value) implements Command {
        @Override
        public String getAction() {
            return "fill";
        }
    }

    public record Press(String selector, String value) implements Command {
        @Override
        public String getAction() {
            return "press";
        }
    }

    public record Clear(String selector) implements Command {

        @Override
        public String getAction() {
            return "clear";
        }
    }

    public record Hover(String selector) implements Command {

        @Override
        public String getAction() {
            return null;
        }
    }

    public record MouseUp() implements Command {


        @Override
        public String getAction() {
            return "mouseUp";
        }
    }

    public record MouseDown() implements Command {

        @Override
        public String getAction() {
            return "mouseDown";
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExpectVisible(String selector, @Nullable Map<String, Object> options) implements Command {
        @Override
        public String getAction() {
            return "expectVisible";
        }
    }

    public record ExpectHidden(String selector) implements Command {
        @Override
        public String getAction() {
            return "expectHidden";
        }
    }

    public record ExpectToContainText(String selector, String value) implements Command {
        @Override
        public String getAction() {
            return "expectToContainText";
        }
    }

    public record UploadGeneratedFile(String selector, FileProperties fileProperties) implements Command {
        @Override
        public String getAction() {
            return "uploadGeneratedFile";
        }
    }

    public record DragAndDrop(String selector, String targetSelector) implements Command {
        @Override
        public String getAction() {
            return "dragAndDrop";
        }
    }

    public record Navigate(String url) implements Command {

        @Override
        public String getAction() {
            return "navigate";
        }
    }


    // ------------------- File Properties Definitions -------------------

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ImageFileProperties.class, name = "image"),
            @JsonSubTypes.Type(value = TextFileProperties.class, name = "text"),
            @JsonSubTypes.Type(value = ExcelFileProperties.class, name = "excel")
    })
    public sealed interface FileProperties {
        String type();
        String fileName();
        @SuppressWarnings("unused")
        String mimeType();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ImageFileProperties(
            String type,
            String fileName,
            String mimeType,
            int width,
            int height,
            @Nullable String text,
            @Nullable String backgroundColor,
            @Nullable String textColor
    ) implements FileProperties {}

    public record TextFileProperties(
            String type,
            String fileName,
            String mimeType,
            String content
    ) implements FileProperties {}

    public record ExcelFileProperties(
            String type,
            String fileName,
            String mimeType,
            String csvContent
    ) implements FileProperties {}


    // ------------------- Parser Utility Class -------------------

    @SneakyThrows
    public Action parseAction(String output) {
        var parts = output.split("\n", 2);
        var action = parts[0];
        var json = parts[1];
        return switch (action) {
            case "STEP" -> Utils.readJSONString(json, StepAction.class);
            case "REJECT" -> Utils.readJSONString(json, RejectAction.class);
            case "ACCEPT" -> Utils.readJSONString(json, AcceptAction.class);
            case "ABORT" -> Utils.readJSONString(json, AbortAction.class);
            default -> throw new IllegalArgumentException("Unknown action type: " + action);
        };
    }

    @SneakyThrows
    private void logTestInfo(String testId, int epoch, String consoleLogs, String dom, byte[] screenshot) {
        if (!logOn)
            return;
        var dir = Objects.requireNonNull(testLogDir).resolve(testId).resolve(Integer.toString(epoch));
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("console-logs.txt"), consoleLogs);
        Files.writeString(dir.resolve("index.html"), dom);
        Files.write(dir.resolve("screenshot.png"), screenshot);
    }

    private String getTestId() {
        return DF.format(new Date());
    }

    public record ExecuteResult(
            boolean successful,
            @javax.annotation.Nullable String errorMessage
    ) {

        private static final ExecuteResult success = new ExecuteResult(true, null);

        public static ExecuteResult failed(String errorMessage) {
            return new ExecuteResult(false, errorMessage);
        }

        public static ExecuteResult success() {
            return success;
        }

    }
}
