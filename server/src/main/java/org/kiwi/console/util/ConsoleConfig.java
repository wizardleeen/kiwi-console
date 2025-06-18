package org.kiwi.console.util;

import org.kiwi.console.genai.DeployClient;
import org.kiwi.console.genai.GeminiAgent;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.util.Map;
import java.util.Objects;

@Component
@Configurable
public class ConsoleConfig {
    public static String CONFIG_PATH;

    private final GeminiConfig geminiConfig;

    private final KiwiConfig kiwiConfig;

    private final ServerConfig serverConfig;

    /** @noinspection unchecked*/
    public ConsoleConfig() {
        var config = getConfig();
        serverConfig = buildServerConfig((Map<String, Object>) config.get("server"));
        geminiConfig = buildGeminiConfig((Map<String, Object>) config.get("gemini"));
        kiwiConfig = buildKiwiConfig((Map<String, Object>) config.get("kiwi"));
        var proxyConfig = config.get("proxy");
        if (proxyConfig != null) {
            //noinspection rawtypes
            if (proxyConfig instanceof Map map)
                configProxy(map);
            else
                throw new ConfigException("Invalid proxy config");
        }
    }

    private void configProxy(Map<String, Object> config) {
        var pacFileUrl = config.get("pacFileUrl");
        if (pacFileUrl != null)
            ProxyUtils.setupPacProxy((String) pacFileUrl);
    }

    private GeminiConfig buildGeminiConfig(Map<String, Object> config) {
        if (config == null)
            throw new ConfigException("Missing gemini config");
        if (!(config.get("apikey") instanceof String apikey))
            throw new ConfigException("Missing gemini.apikey config");
        return new GeminiConfig(apikey);
    }

    private KiwiConfig buildKiwiConfig(Map<String, Object> config) {
        if (config == null)
            throw new ConfigException("Missing kiwi config");
        if (!(config.get("host") instanceof String host))
            throw new ConfigException("Missing kiwi.host config");
        return new KiwiConfig(host);
    }


    private ServerConfig buildServerConfig(Map<String, Object> config) {
        if (config == null)
            throw new RuntimeException("server config is missing");
        var port = (int) config.getOrDefault("port", 8080);
        return new ServerConfig(port);
    }

    private Map<String, Object> getConfig() {
        Objects.requireNonNull(CONFIG_PATH, "Config path is not specified");
        var yaml = new Yaml();
        try (var inputStream = new FileInputStream(CONFIG_PATH)) {
            return yaml.load(inputStream);
        } catch (YAMLException e) {
            throw new RuntimeException("Error parsing YAML file: " + CONFIG_PATH, e);
        } catch (Exception e) {
            throw new RuntimeException("An error occurred: " + e.getMessage(), e);
        }
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
    public DeployClient deployClient() {
        return new DeployClient(kiwiConfig.host);
    }

    public record ServerConfig(int port) {}

    public record GeminiConfig(String apiKey) {}

    private record KiwiConfig(String host) {}

}
