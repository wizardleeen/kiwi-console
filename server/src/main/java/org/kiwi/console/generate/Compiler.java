package org.kiwi.console.generate;

import java.nio.file.Path;
import java.util.List;

public interface Compiler {

    DeployResult run(long appId, List<SourceFile> sourceFiles, List<Path> removedFiles);

    List<SourceFile> getSourceFiles(long appId);

    void addFile(long appId, SourceFile file);

    DeployResult deploy(long appId);

    void commit(long appId, String message);

    void reset(long appId, String templateRepo, String branch);

    void delete(long appId);

    void revert(long appId);

}
