package org.kiwi.console;

import com.google.debugging.sourcemap.SourceMapConsumerV3;
import com.google.debugging.sourcemap.proto.Mapping.OriginalMapping;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StackTraceDeobfuscator {

    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile("(?<file>\\S+):(?<line>\\d+):(?<col>\\d+)");

    private static final String sourceMapPath = "/tmp/index-WqIyrpjh.js.map";
    private static final String stackTracePath = "/tmp/log.json";

    @SneakyThrows
    public static void main(String[] args) {
        // 1. Read file contents
        String sourceMapContent = Files.readString(Paths.get(sourceMapPath));
        String stackTraceContent = Files.readString(Paths.get(stackTracePath));

        // 3. Process the stack trace
        System.out.println("--- Original Stack Trace ---");
        System.out.println(stackTraceContent);
        System.out.println("\n--- De-obfuscated Stack Trace ---");

        String deobfuscatedTrace = deobfuscate(sourceMapContent, stackTraceContent);
        System.out.println(deobfuscatedTrace);
    }

    @SneakyThrows
    public static String deobfuscate(String sourceMap, String consoleLog) {
        SourceMapConsumerV3 consumer = new SourceMapConsumerV3();
        consumer.parse(sourceMap);
        return deobfuscate0(consoleLog, consumer);
    }

    private static String deobfuscate0(String stackTrace, SourceMapConsumerV3 consumer) {
        StringBuilder result = new StringBuilder();
        String[] lines = stackTrace.split("\n");

        for (String line : lines) {
            Matcher matcher = STACK_TRACE_PATTERN.matcher(line);
            StringBuffer lineBuffer = new StringBuffer();

            // Find all occurrences of file:line:col in the line
            while (matcher.find()) {
                try {
                    int lineNumber = Integer.parseInt(matcher.group("line"));
                    int columnNumber = Integer.parseInt(matcher.group("col"));

                    // Query the source map for the original mapping
                    OriginalMapping mapping = consumer.getMappingForLine(lineNumber, columnNumber);

                    if (mapping != null && mapping.getOriginalFile() != null) {
                        // Replace the minified location with the original one
                        String replacement = String.format("%s:%d:%d (%s)",
                                mapping.getOriginalFile(),
                                mapping.getLineNumber(),
                                mapping.getColumnPosition(),
                                mapping.getIdentifier() != null ? "function '" + mapping.getIdentifier() + "'" : "unknown function");
                        matcher.appendReplacement(lineBuffer, Matcher.quoteReplacement(replacement));
                    } else {
                        // If no mapping found, keep the original
                        matcher.appendReplacement(lineBuffer, matcher.group(0));
                    }
                } catch (NumberFormatException e) {
                    // Should not happen with the regex, but good practice
                    matcher.appendReplacement(lineBuffer, matcher.group(0));
                }
            }
            matcher.appendTail(lineBuffer);
            result.append(lineBuffer).append("\n");
        }
        return result.toString();
    }
}