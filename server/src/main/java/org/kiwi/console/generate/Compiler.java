package org.kiwi.console.generate;

import java.nio.file.Path;
import java.util.List;

public interface Compiler {

    DeployResult run(long appId, String projectName, List<SourceFile> sourceFiles, List<Path> removedFiles, boolean deploySource, boolean noBackup);

    List<SourceFile> getSourceFiles(String projectName);

    void addFile(String projectName, SourceFile file);

    DeployResult deploy(long appId, String projectName, boolean deploySource, boolean noBackup);

    void commit(String projectName, String message);

    void reset(String projectName, String templateRepo, String branch);

    void delete(String projectName);

    void revert(long appId, String projectName, boolean deploySource);

}
