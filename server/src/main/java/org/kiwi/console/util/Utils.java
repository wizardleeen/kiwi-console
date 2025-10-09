package org.kiwi.console.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.gson.GsonBuilder;
import feign.Feign;
import feign.RequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import jakarta.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.generate.GenerationService;
import org.kiwi.console.schema.dto.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Slf4j
public class Utils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonGenerator.Feature.IGNORE_UNKNOWN)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final ObjectMapper OBJECT_MAPPER_IGNORE_NULL = new ObjectMapper()
            .enable(JsonGenerator.Feature.IGNORE_UNKNOWN)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static final ObjectMapper INDENT_OBJECT_MAPPER = new ObjectMapper();

    public static final Pattern DIGITS_PTN = Pattern.compile("\\d+");

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH::mm:ss");

    static {
        OBJECT_MAPPER.registerModule(new Jdk8Module());
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        OBJECT_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        OBJECT_MAPPER_IGNORE_NULL.registerModule(new Jdk8Module());
        OBJECT_MAPPER_IGNORE_NULL.registerModule(new JavaTimeModule());
        OBJECT_MAPPER_IGNORE_NULL.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        OBJECT_MAPPER_IGNORE_NULL.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        OBJECT_MAPPER_IGNORE_NULL.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        SimpleModule module = new SimpleModule();
        INDENT_OBJECT_MAPPER.registerModule(module);
        INDENT_OBJECT_MAPPER.registerModule(new Jdk8Module());
        INDENT_OBJECT_MAPPER.registerModule(new JavaTimeModule());
        INDENT_OBJECT_MAPPER.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        INDENT_OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        INDENT_OBJECT_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    @SneakyThrows
    public static <T> T readJSONString(String jsonStr, Class<T> type) {
        return OBJECT_MAPPER.readValue(jsonStr, type);
    }

    public static String toJSONString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to write JSON string, object: " + object, e);
        }
    }

    public static String toPrettyJSONString(Object object) {
        try {
            return INDENT_OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to write JSON string, object: " + object, e);
        }
    }

    public static Object readJSONString(String jsonStr, Type type) {
        try {
            var reader = OBJECT_MAPPER.reader().forType(type);
            return reader.readValue(jsonStr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to read JSON string, JSON string: " + jsonStr, e);
        }
    }

    public static <T> T readJSONString(String jsonStr, TypeReference<T> typeReference) {
        try {
            var reader = OBJECT_MAPPER.reader().forType(typeReference);
            return reader.readValue(jsonStr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to read JSON string, JSON string: " + jsonStr, e);
        }
    }



    public static <T, R> R safeCall(@Nullable T t, Function<T, R> f) {
        if (t != null)
            return f.apply(t);
        else
            return null;
    }

    public static void removeDirectory(String path) {
        removeDirectory(Path.of(path));
    }

    @SneakyThrows
    public static void removeDirectory(Path path) {
        clearDirectory(path);
        Files.delete(path);
    }

    public static void clearDirectory(String path) {
        clearDirectory(Path.of(path));
    }

    public static void clearDirectory(Path path) {
        if (Files.exists(path)) {
            try (var files = Files.walk(path)) {
                //noinspection ResultOfMethodCallIgnored
                files.sorted(Comparator.reverseOrder())
                        .filter(p -> !p.equals(path))
                        .forEach(f -> f.toFile().delete());
            } catch (IOException e) {
                System.err.println("Faileded to clear directory '" + path + "'");
                System.exit(1);
            }
        }
    }

    public static CommandResult executeCommand(Path directory, String... command) {
        return executeCommand(directory, Arrays.asList(command));
    }

    public static CommandResult executeCommand(Path directory, List<String> commands) {
        log.info("Executing command {} in working dir {}",
                String.join(" ", commands), directory.toString());
        ProcessBuilder processBuilder = new ProcessBuilder(commands);

        // Set the working directory for the command
        processBuilder.directory(directory.toFile());

        // Merge the standard output and standard error streams.
        // This makes it easier to capture all output.
        processBuilder.redirectErrorStream(true);

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Use a StringBuilder to capture the command's output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Wait for the process to complete (with a timeout)
        boolean finished;
        try {
            finished = process.waitFor(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Command timed out.");
        }

        return new CommandResult(process.exitValue(), output.toString());
    }

    public static void require(boolean condition) {
        if (!condition)
            throw new AssertionError();
    }

    public static void require(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    public static String getFirstLine(String s) {
        var buf = new StringBuilder();
        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            if (c == '\r' || c == '\n')
                break;
            buf.append(c);
        }
        return buf.toString();
    }

    @SneakyThrows
    public static <T> T readJsonBytes(InputStream input, Class<T> cls) {
        return OBJECT_MAPPER.readValue(input, cls);
    }

    public static String loadResource(String file) {
        try (var input = GenerationService.class.getResourceAsStream(file)) {
            return new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public record CommandResult(int exitCode, String output) {}

    public static <T> T createKiwiFeignClient(String url, Class<T> type, long appId) {
        return createFeignClient(url, type, rt -> {
            rt.header("X-App-ID", Long.toString(appId));
            rt.header("X-Refresh-Policy", "none");
        });
    }

    private static final GsonEncoder gsonEncoder;
    private static final GsonDecoder gsonDecoder;
    private static final FeignErrorDecoder feignErrorDecoder = new FeignErrorDecoder();

    static {
        var typeFactory = RuntimeTypeAdapterFactory
                .of(TypeDTO.class, "kind")
                .registerSubtype(PrimitiveTypeDTO.class, "primitive")
                .registerSubtype(ClassTypeDTO.class, "class")
                .registerSubtype(ArrayTypeDTO.class, "array")
                .registerSubtype(UnionTypeDTO.class, "union");

        var gson = new GsonBuilder()
                .registerTypeAdapterFactory(typeFactory)
                .setPrettyPrinting()
                .create();

        gsonEncoder = new GsonEncoder(gson);
        gsonDecoder = new GsonDecoder(gson);
    }

    public static <T> T createFeignClient(String url, Class<T> type) {
        return createFeignClient(url, type, null);
    }

    public static <T> T createFeignClient(String url, Class<T> type, @Nullable RequestInterceptor interceptor) {
        var builder = Feign.builder()
                .encoder(gsonEncoder)
                .decoder(gsonDecoder)
                .errorDecoder(feignErrorDecoder);
        if (interceptor != null)
            builder = builder.requestInterceptor(interceptor);
        return builder.target(type, url);
    }

    public static <T, R> List<R> map(List<T> list, Function<T, R> mapper) {
        var result = new ArrayList<R>();
        for (T t : list) {
            result.add(mapper.apply(t));
        }
        return result;
    }

    public static String getResourcePath(String resourcePath) {
        // Ensure the path uses forward slashes, standard for resources
        String normalizedPath = resourcePath.replace('\\', '/');
        // Remove leading slash if present, as getResource expects a path relative to the root
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        // Use the class loader of the current class to find the resource
        URL resourceUrl = Utils.class.getClassLoader().getResource(normalizedPath);
        Objects.requireNonNull(resourceUrl, "Resource not found in classpath: " + normalizedPath);

        try {
            URI resourceUri = resourceUrl.toURI();
            // Handle resources potentially inside JAR files (though less common for src/test/resources during tests)
            // For regular file system resources, this works directly.
            Path path = Paths.get(resourceUri);
            return path.toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI syntax for resource: " + normalizedPath, e);
        } catch (java.nio.file.FileSystemNotFoundException e) {
            // Handle case where the resource might be inside a JAR/ZIP (less common for src/test/resources)
            // This might require more complex handling depending on how 'deploy' consumes the path.
            // For now, assume 'deploy' needs a standard file path. If it can handle URLs or InputStreams, that's better.
            System.err.println("Warning: Resource might be inside a JAR/ZIP, returning URL path: " + resourceUrl.getPath());
            return resourceUrl.getPath(); // Fallback, might not work if 'deploy' strictly needs a file system path
        }
    }

    public static String escapeJavaString(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                // You can add other cases here if needed, for example for non-printable characters.
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    public static <T> int count(Iterable<T> it, Predicate<T> filter) {
        var cnt = 0;
        for (T t : it) {
            if (filter.test(t))
                cnt++;
        }
        return cnt;
    }

    public static <T> @Nullable T find(Iterable<T> it, Predicate<T> filter) {
        for (T t : it) {
            if (filter.test(t))
                return t;
        }
        return null;
    }

    public static <T> T findRequired(Iterable<T> it, Predicate<T> filter) {
        return findRequired(it, filter, NoSuchElementException::new);
    }

    public static <T> T findRequired(Iterable<T> it, Predicate<T> filter, Supplier<? extends RuntimeException> exceptionSupplier) {
        for (T t : it) {
            if (filter.test(t))
                return t;
        }
        throw exceptionSupplier.get();
    }

    public static <T, K> Map<K, T> toMap(Iterable<T> it, Function<T, K> keyMapper) {
        return toMap(it, keyMapper, Function.identity());
    }

    public static <T, K, V> Map<K, V> toMap(Iterable<T> it, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        var map = new LinkedHashMap<K, V>();
        for (T t : it) {
            if (map.put(keyMapper.apply(t), valueMapper.apply(t)) != null)
                throw new IllegalStateException("Duplicate key");
        }
        return map;
    }

    public static <K, V> Map<K, V> toMap(Collection<K> keys, Iterable<V> values) {
        var map = new LinkedHashMap<K, V>();
        var it1 = keys.iterator();
        var it2 = values.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            map.put(it1.next(), it2.next());
        }
        if (it1.hasNext() || it2.hasNext())
            throw new IllegalStateException("Keys and values have different sizes");
        return map;
    }

}
