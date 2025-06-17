package org.kiwi.console.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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


    public static <T> T readJSONString(String jsonStr, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(jsonStr, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to read JSON string, JSON string: " + jsonStr, e);
        }
    }

    public static String toJSONString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
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
        log.info("Executing command {} in working dir {}",
                String.join(" ", command), directory.toString());
        ProcessBuilder processBuilder = new ProcessBuilder(command);

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

    public record CommandResult(int exitCode, String output) {}

}
