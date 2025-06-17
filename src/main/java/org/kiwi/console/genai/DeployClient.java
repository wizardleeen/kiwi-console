package org.kiwi.console.genai;

import org.kiwi.console.util.ParameterizedTypeImpl;
import org.kiwi.console.util.Result;
import org.kiwi.console.util.TypeReference;
import org.kiwi.console.util.Utils;

import java.io.InputStream;
import java.net.CookieHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DeployClient implements DeployService {

    private final String host;
    private final HttpClient client;
    public static final String X_APP_ID = "X-App-ID";

    public DeployClient(String host) {
        this.host = host;
        client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    @Override
    public String deploy(long appId, String token, InputStream input) {
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
            return processResult(resp.body(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <R> R processResult(String responseStr, TypeReference<R> responseTypeRef) {
        //noinspection unchecked
        var result = (Result<R>) Utils.readJSONString(responseStr,
                ParameterizedTypeImpl.create(Result.class, responseTypeRef.getGenericType()));
        if (!result.isSuccessful())
            throw new RuntimeException(result.message());
        return result.data();
    }

}
