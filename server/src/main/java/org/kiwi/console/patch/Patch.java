package org.kiwi.console.patch;

import org.kiwi.console.generate.SourceFile;

import java.nio.file.Path;
import java.util.List;

public record Patch(List<SourceFile> addedFiles, List<Path> removedFiles) {
}
