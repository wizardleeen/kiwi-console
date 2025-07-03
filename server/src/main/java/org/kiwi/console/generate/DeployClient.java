package org.kiwi.console.generate;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kiwi.console.util.Result;
import org.kiwi.console.util.Utils;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DeployClient implements DeployService {

    private final String host;
    private final HttpClient client;
    public static final String X_APP_ID = "X-App-ID";

    public DeployClient(String host, HttpClient client) {
        this.host = host;
        this.client = client;
    }

    @Override
    public Result<String> deploy(long appId, String token, InputStream input) {
        try {
            var uri = new URI(host + "/type/deploy/" + appId);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Cookie", "__token_2__=" + token)
                    .header(X_APP_ID, "2")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(input.readAllBytes()))
                    .build();
            var resp = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return Utils.readJSONString(resp.body(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
