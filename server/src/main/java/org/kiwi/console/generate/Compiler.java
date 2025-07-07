package org.kiwi.console.generate;

import jakarta.annotation.Nullable;

import java.util.List;

public interface Compiler {

    DeployResult run(long appId, List<SourceFile> sourceFiles);

    DeployResult deploy(long appId);

    void commit(long appId, String message);

    void reset(long appId);

    @Nullable
    String getCode(long appId, String fileName);

    void delete(long appId);
}
