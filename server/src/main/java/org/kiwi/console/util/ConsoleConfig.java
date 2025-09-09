package org.kiwi.console.util;

import org.kiwi.console.browser.Browser;
import org.kiwi.console.browser.PlaywrightBrowser;
import org.kiwi.console.file.UrlFetcher;
import org.kiwi.console.generate.*;
import org.kiwi.console.generate.claude.ClaudeModel;
import org.kiwi.console.generate.k2.K2Model;
import org.kiwi.console.generate.qwen.QwenModel;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.file.FileService;
import org.kiwi.console.object.ObjectClient;
import org.kiwi.console.schema.SchemaClient;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Component
@Configurable
public class ConsoleConfig {
    public static String CONFIG_PATH;

    private final ApiKeys apiKeys;
    private final KiwiConfig kiwiConfig;
    private final ServerConfig serverConfig;
    private final PageConfig pageConfig;
    private final UrlTemplates urlTemplates;
    private final UploadConfig uploadConfig;
    private final TestConfig testConfig;

    public ConsoleConfig() {
        var config = getConfig();
        serverConfig = buildServerConfig(config);
        apiKeys = buildApiKeys(config);
        kiwiConfig = buildKiwiConfig(config);
        pageConfig = buildPageConfig(config);
        urlTemplates = buildUrlTemplates(config);
        uploadConfig = buildUploadConfig(config);
        testConfig = buildTestConfig(config);
        configProxy(config);
    }

    private TestConfig buildTestConfig(YmlConfig config) {
        return new TestConfig(config.getString("test", "env-dir"));
    }

    private UploadConfig buildUploadConfig(YmlConfig config) {
        return new UploadConfig(
                config.getString("upload", "dir"),
                config.getString("upload", "sourcemap")
        );
    }

    private PageConfig buildPageConfig(YmlConfig config) {
        return new PageConfig(config.getString("page", "works-dir"));
    }

    private void configProxy(YmlConfig config) {
        var pacFileUrl = config.tryGetString("proxy", "pac");
        if (pacFileUrl != null)
            ProxyUtils.setupPacProxy(pacFileUrl);
    }

    private ApiKeys buildApiKeys(YmlConfig config) {
        return new ApiKeys(
                config.getString("apikeys", "gemini"),
                config.getString("apikeys", "gemini2"),
                config.getString("apikeys", "claude"),
                config.getString("apikeys", "k2"),
                config.getString("apikeys", "qwen")
        );
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
                config.getString("url-templates", "management"),
                config.getString("url-templates", "source-code")
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
    public Browser browser() {
        return new PlaywrightBrowser();
    }

    @Bean
    public GenerationService generationService(List<Model> models,
                                               KiwiCompiler kiwiCompiler,
                                               PageCompiler pageCompiler,
                                               AppClient appClient,
                                               UserClient userClient,
                                               ExchangeClient exchangeClient,
                                               GenerationConfigClient generationConfigClient,
                                               UrlFetcher urlFetcher,
                                               Browser browser,
                                               AttachmentService attachmentService,
                                               @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        return new GenerationService(
                models,
                kiwiCompiler,
                pageCompiler,
                exchangeClient,
                appClient,
                userClient,
                urlTemplates.product,
                urlTemplates.management,
                urlTemplates.sourceCode,
                generationConfigClient,
                urlFetcher, taskExecutor, browser, attachmentService,
                Path.of(testConfig.envDir)
                );
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

    @Primary
    @Bean
    public GeminiModel gemini2_5_ProModel() {
        return new GeminiModel("gemini-2.5-pro", apiKeys.gemini);
    }

    @Bean
    public GeminiModel gemini2_5_FlashModel() {
        return new GeminiModel("gemini-2.5-flash", apiKeys.gemini2);
    }

    @Bean
    public ClaudeModel claudeModel() {
        return new ClaudeModel(apiKeys.claude);
    }

    @Bean
    public K2Model k2Model() {
        return new K2Model(apiKeys.k2);
    }

    @Bean
    public QwenModel qwenModel() {
        return new QwenModel(apiKeys.qwen);
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
    public AttachmentService attachmentService(FileService fileService, AppClient appClient) {
        return new AttachmentServiceImpl(kiwiConfig.chatAppId, fileService,
                Path.of(uploadConfig.sourcemapDir),
                appClient);
    }

    @Bean
    public AppClient appClient(UserClient userClient) {
        return new AppService(kiwiConfig.host, kiwiConfig.chatAppId, userClient);
    }

    @Bean
    public KiwiUserClient kiwiUserClient() {
        return Utils.createFeignClient(kiwiConfig.host, KiwiUserClient.class);
    }

    @Bean
    public ObjectClient objectClient() {
        return Utils.createFeignClient(kiwiConfig.host, ObjectClient.class);
    }

    @Bean
    public SchemaClient schemaClient() {
        return Utils.createFeignClient(kiwiConfig.host, SchemaClient.class);
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

    public record ApiKeys(
            String gemini,
            String gemini2,
            String claude,
            String k2,
            String qwen
    ) {}

    private record KiwiConfig(String host, long chatAppId, String worksDir) {}

    public record PageConfig(
            String worksDir
    ) {}

    private record UrlTemplates(
            String product,
            String management,
            String sourceCode
    ) {}

    private record UploadConfig(
        String dir,
        String sourcemapDir
    ) {}

    private record TestConfig(
        String envDir
    ) {}

}
