package org.kiwi.console.generate;

import java.nio.file.Path;
import java.util.List;

public interface Compiler {

    DeployResult run(long appId, List<SourceFile> sourceFiles, List<Path> removedFiles);

    List<SourceFile> getSourceFiles(long appId);

    DeployResult deploy(long appId);

    void commit(long appId, String message);

    void reset(long appId);

    void delete(long appId);
}
