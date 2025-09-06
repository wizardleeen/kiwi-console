package org.kiwi.console.browser;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import lombok.SneakyThrows;
import org.kiwi.console.util.PortProcessKiller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class PlaywrightBrowser implements Browser {
    private final com.microsoft.playwright.Browser browser;

    @SneakyThrows
    public PlaywrightBrowser() {
        PortProcessKiller.killProcessOnPort(9222);
        var playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--remote-debugging-port=9222"))
        );
    }

    @Override
    public Page createPage() {
        return new PlaywrightPage(browser.newContext());
    }

    private static String getBrowserWsEndpoint(String host, int port) throws Exception {
        System.out.println("[Agent] Discovering browser WebSocket endpoint...");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://%s:%d/json/version", host, port)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get browser version info. Status code: " + response.statusCode());
        }

        JsonObject versionInfo = new Gson().fromJson(response.body(), JsonObject.class);
        String wsEndpoint = versionInfo.get("webSocketDebuggerUrl").getAsString();
        if (wsEndpoint == null || wsEndpoint.isEmpty()) {
            throw new RuntimeException("Could not find 'webSocketDebuggerUrl' in browser version info.");
        }

        System.out.println("[Agent] ✅ Discovered endpoint: " + wsEndpoint);
        return wsEndpoint;
    }


}
