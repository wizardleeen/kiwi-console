package org.kiwi.console.util;

import org.kiwi.console.file.UrlFetcher;
import org.kiwi.console.generate.*;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.file.FileService;
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
    private final PageConfig pageConfig;
    private final UrlTemplates urlTemplates;
    private final UploadConfig uploadConfig;


    public ConsoleConfig() {
        var config = getConfig();
        serverConfig = buildServerConfig(config);
        geminiConfig = buildGeminiConfig(config);
        kiwiConfig = buildKiwiConfig(config);
        pageConfig = buildPageConfig(config);
        urlTemplates = buildUrlTemplates(config);
        uploadConfig = buildUploadConfig(config);
        configProxy(config);
    }

    private UploadConfig buildUploadConfig(YmlConfig config) {
        return new UploadConfig(config.getString("upload", "dir"));
    }

    private PageConfig buildPageConfig(YmlConfig config) {
        return new PageConfig(config.getString("page", "works-dir"));
    }

    private void configProxy(YmlConfig config) {
        var pacFileUrl = config.tryGetString("proxy", "pac");
        if (pacFileUrl != null)
            ProxyUtils.setupPacProxy(pacFileUrl);
    }

    private GeminiConfig buildGeminiConfig(YmlConfig config) {
        return new GeminiConfig(config.getString("gemini", "apikey"));
    }

    private KiwiConfig buildKiwiConfig(YmlConfig config) {
        return new KiwiConfig(config.getString("kiwi", "host"),
            config.getLong("kiwi", "chat-app-id"),
            config.getString("kiwi", "works-dir")
        );
    }

    private ServerConfig buildServerConfig(YmlConfig config) {
        var port = Objects.requireNonNullElse(config.tryGetInt("server", "port"), 8080);
        return new ServerConfig(port);
    }

    private UrlTemplates buildUrlTemplates(YmlConfig config) {
        return new UrlTemplates(
                config.getString("url-templates", "product"),
                config.getString("url-templates", "management")
        );
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
    public GenerationConfigClient generationConfigClient() {
        return Utils.createKiwiFeignClient(kiwiConfig.host, GenerationConfigClient.class, kiwiConfig.chatAppId);
    }

    @Bean
    public GenerationService generationService(GeminiAgent geminiAgent,
                                               KiwiCompiler kiwiCompiler,
                                               PageCompiler pageCompiler,
                                               AppClient appClient,
                                               UserClient userClient,
                                               ExchangeClient exchangeClient,
                                               GenerationConfigClient generationConfigClient,
                                               UrlFetcher urlFetcher,
                                               @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        return new GenerationService(
                geminiAgent,
                kiwiCompiler,
                pageCompiler,
                exchangeClient,
                appClient,
                userClient,
                urlTemplates.product,
                urlTemplates.management,
                generationConfigClient,
                urlFetcher,
                taskExecutor);
    }

    @Bean
    public FileService uploadService() {
        return new FileService(Path.of(uploadConfig.dir));
    }

    @Bean
    public KiwiCompiler kiwiCompiler(DeployService deployService) {
        return new DefaultKiwiCompiler(Path.of(kiwiConfig.worksDir), deployService);
    }

    @Bean
    public PageCompiler pageCompiler() {
        return new DefaultPageCompiler(Path.of(pageConfig.worksDir));
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
        return Utils.createKiwiFeignClient(kiwiConfig.host, ExchangeClient.class, kiwiConfig.chatAppId);
    }

    @Bean
    public AttachmentService attachmentService(FileService fileService) {
        return new AttachmentServiceImpl(kiwiConfig.chatAppId, fileService);
    }

    @Bean
    public AppClient appClient(UserClient userClient) {
        return new AppService(kiwiConfig.host, kiwiConfig.chatAppId, userClient);
    }

    @Bean
    public SysUserClient sysUserClient() {
        return Utils.createFeignClient(kiwiConfig.host, SysUserClient.class);
    }

    @Bean
    public UserClient userClient() {
        return new UserService(kiwiConfig.host, kiwiConfig.chatAppId);
    }

    @Bean
    public DeployClient deployClient(HttpClient client) {
        return new DeployClient(kiwiConfig.host, client);
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    @Bean
    public UrlFetcher urlFetcher() {
        return new UrlFetcher(Format.format(urlTemplates.product(), Long.toString(kiwiConfig.chatAppId())));
    }

    public record ServerConfig(int port) {}

    public record GeminiConfig(String apiKey) {}

    private record KiwiConfig(String host, long chatAppId, String worksDir) {}

    public record PageConfig(
            String worksDir
    ) {}

    private record UrlTemplates(
            String product,
            String management
    ) {}

    private record UploadConfig(
        String dir
    ) {}

}
