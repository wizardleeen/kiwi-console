package org.kiwi.console.util;

import lombok.SneakyThrows;
import org.kiwi.console.generate.Format;
import org.kiwi.console.generate.SourceFile;
import org.kiwi.console.patch.PatchReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PromptTemplateGen {

    private static final Path path = Path.of("/Users/leen/workspace/kiwi-console/server/src/main/resources/prompt");

    @SneakyThrows
    public static void main(String[] args) {
        var example = loadExampleCode();
        try (var s = Files.walk(path)) {
            s.forEach(f -> {
                if (f.getFileName().toString().contains("-meta")) {
                    var instPath = f.getParent().resolve(f.getFileName().toString().replace("-meta", ""));
                    try {
                        var content = Files.readString(f);
                        var subst = Format.formatKeyed(content, "example", example);
                        Files.writeString(instPath, subst);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    private static String loadExampleCode() {
        return PatchReader.buildCode(SourceFile.getAllSourceFiles(Path.of(Utils.getResourcePath("/example_project"))));
    }



}
