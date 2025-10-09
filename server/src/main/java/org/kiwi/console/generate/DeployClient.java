package org.kiwi.console.generate;

import lombok.SneakyThrows;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.ErrorResponse;
import org.kiwi.console.util.Utils;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DeployClient implements DeployService {

    private final String host;
    private final HttpClient client;

    public DeployClient(String host, HttpClient client) {
        this.host = host;
        this.client = client;
    }

    @SneakyThrows
    @Override
    public String deploy(long appId, boolean noBackup, InputStream input) {
        var uri = new URI(host + "/internal-api/deploy/" + appId + "?no-backup=" + noBackup);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(input.readAllBytes()))
                .build();
        var resp = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return processResponse(resp, String.class);
    }

    @SneakyThrows
    @Override
    public String getDeployStatus(long appId, String deployId) {
        var uri = new URI(host + "/internal-api/deploy/status/" + appId + "/" + deployId);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();
        var resp = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return processResponse(resp, String.class);
    }

    private <R> R processResponse(HttpResponse<String> resp, Class<R> cls) {
        if (resp.statusCode() != 200) {
            var errorResp = Utils.readJSONString(resp.body(), ErrorResponse.class);
            throw new BusinessException(ErrorCode.DEPLOY_FAILED, errorResp.message());
        }
        if (cls == String.class)
            //noinspection unchecked
            return (R) resp.body();
        else
            return Utils.readJSONString(resp.body(), cls);
    }

    @SneakyThrows
    @Override
    public void revert(long appId) {
        var uri = new URI(host + "/internal-api/deploy/revert/" + appId);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        var resp = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            var errorResp = Utils.readJSONString(resp.body(), ErrorResponse.class);
            throw new BusinessException(ErrorCode.DEPLOY_FAILED, errorResp.message());
        }
    }

}
