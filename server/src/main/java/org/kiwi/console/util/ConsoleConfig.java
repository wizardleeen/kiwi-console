package org.kiwi.console.util;

import org.kiwi.console.generate.*;
import org.kiwi.console.kiwi.ApplicationClient;
import org.kiwi.console.kiwi.ApplicationService;
import org.kiwi.console.kiwi.ExchangeClient;
import org.kiwi.console.kiwi.UserClient;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Objects;

@Component
@Configurable
public class ConsoleConfig {
    public static String CONFIG_PATH;

    private final GeminiConfig geminiConfig;

    private final KiwiConfig kiwiConfig;

    private final ServerConfig serverConfig;
    private final AppConfig appConfig;

    public ConsoleConfig() {
        var config = getConfig();
        serverConfig = buildServerConfig(config);
        geminiConfig = buildGeminiConfig(config);
        kiwiConfig = buildKiwiConfig(config);
        appConfig = buildAppConfig(config);
        configProxy(config);
    }

    private AppConfig buildAppConfig(YmlConfig config) {
        return new AppConfig(
                config.getString("app", "kiwi-works-dir"),
                config.getString("app", "page-works-dir"),
                config.getString("app", "product-url-template"),
                config.getString("app", "management-url-template")
        );
    }

    private void configProxy(YmlConfig config) {
        var pacFileUrl = config.tryGetString("proxy", "pac-file-url");
        if (pacFileUrl != null)
            ProxyUtils.setupPacProxy(pacFileUrl);
    }

    private GeminiConfig buildGeminiConfig(YmlConfig config) {
        return new GeminiConfig(config.getString("gemini", "apikey"));
    }

    private KiwiConfig buildKiwiConfig(YmlConfig config) {
        return new KiwiConfig(config.getString("kiwi", "host"),
                config.getLong("kiwi", "sys-app-id"),
                config.getString("kiwi", "token"));
    }

    private ServerConfig buildServerConfig(YmlConfig config) {
        var port = Objects.requireNonNullElse(config.tryGetInt("server", "port"), 8080);
        return new ServerConfig(port);
    }

    private YmlConfig getConfig() {
        Objects.requireNonNull(CONFIG_PATH, "Config path is not specified");
        var yaml = new Yaml();
        try (var inputStream = new FileInputStream(CONFIG_PATH)) {
            return new YmlConfig(yaml.load(inputStream));
        } catch (YAMLException e) {
            throw new RuntimeException("Error parsing YAML file: " + CONFIG_PATH, e);
        } catch (Exception e) {
            throw new RuntimeException("An error occurred: " + e.getMessage(), e);
        }
    }

    @Bean
    public GenerationService generationService(GeminiAgent geminiAgent,
                                               KiwiCompiler kiwiCompiler,
                                               PageCompiler pageCompiler,
                                               ApplicationClient applicationClient,
                                               ExchangeClient exchangeClient,
                                               @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        return new GenerationService(
                geminiAgent,
                kiwiCompiler,
                pageCompiler,
                exchangeClient,
                applicationClient,
                appConfig.productUrlTemplate,
                appConfig.managementUrlTemplate,
                kiwiConfig.token,
                taskExecutor
        );
    }

    @Bean
    public KiwiCompiler kiwiCompiler(DeployService deployService) {
        return new DefaultKiwiCompiler(Path.of(appConfig.kiwiWorksDir), deployService);
    }

    @Bean
    public PageCompiler pageCompiler() {
        return new DefaultPageCompiler(Path.of(appConfig.pageWorksDir));
    }

    @Bean
    public GeminiAgent geminiAgent() {
        return new GeminiAgent(geminiConfig.apiKey);
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
        return factory -> factory.setPort(serverConfig.port());
    }

    @Bean
    public ExchangeClient exchangeClient() {
        return Utils.createKiwiFeignClient(kiwiConfig.host, ExchangeClient.class, kiwiConfig.sysAppId);
    }

    @Bean
    public ApplicationClient applicationClient() {
        return new ApplicationService(kiwiConfig.host, kiwiConfig.sysAppId(), kiwiConfig.token);
    }

    @Bean
    public UserClient userClient() {
        return Utils.createKiwiFeignClient(kiwiConfig.host, UserClient.class, kiwiConfig.sysAppId);
    }

    @Bean
    public DeployClient deployClient(HttpClient client) {
        return new DeployClient(kiwiConfig.host, client);
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    public record ServerConfig(int port) {}

    public record GeminiConfig(String apiKey) {}

    private record KiwiConfig(String host, long sysAppId, String token) {}

    public record AppConfig(
            String kiwiWorksDir,
            String pageWorksDir,
            String productUrlTemplate,
            String managementUrlTemplate
    ) {}

}
