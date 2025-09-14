package org.kiwi.console.browser;

import lombok.SneakyThrows;

import java.io.IOException;

public class AgentRunner {

    private static final String text = """
            STEP
            Create a new task
            [
              {
                "action": "click",
                "selector": "[data-testid='create-task-button']"
              },
              {
                "action": "fill",
                "selector": "[data-testid='create-task-title-input']",
                "value": "My Test Task4"
              },
              {
                "action": "fill",
                "selector": "[data-testid='create-task-description-input']",
                "value": "This task is for testing the delete button."
              },
              {
                "action": "click",
                "selector": "[data-testid='create-task-submit-button']"
              },
              {
                "action": "expectVisible",
                "selector": "div[data-testid^='task-card-']:has-text('My Test Task4')"
              }
            ]
            """;

    @SneakyThrows
    public static void main(String[] args) throws IOException {
//        var browser = new PlaywrightBrowser();
//        var page = browser.createPage();
//        page.navigate("https://1000211657.metavm.test/");
//        var action = (PlaywrightActions.StepAction) PlaywrightActions.parser.parse(text);
//        for (PlaywrightActions.PlaywrightCommand a : action.getCommands()) {
//            page.execute(a);
//        }
//        System.out.println("Console logs: " + page.getConsoleLogs());
//        Files.write(Path.of("/tmp/test.png"), page.getScreenshot());
//        System.out.println("target ID: " + page.getTargetId());
//        System.out.println("Console logs\n" + page.getConsoleLogs());
//        Files.write(Path.of("/tmp/test.png"), page.getScreenshot());
    }
}