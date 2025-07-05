package org.kiwi.console.generate;

import jakarta.annotation.Nullable;
import lombok.SneakyThrows;
import org.kiwi.console.util.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class AbstractCompiler implements Compiler {

    protected final Path baseDir;

    public AbstractCompiler(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public DeployResult run(long appId, List<SourceFile> sourceFiles) {
        var workDir = getWorkDir(appId);
        sourceFiles.forEach(f -> writeSourceFile(workDir, f));
        var r = build(workDir);
        if (r.successful())
            return deploy(appId);
        else
            return new DeployResult(false, r.message());
    }

    @SneakyThrows
    private void writeSourceFile(WorkDir workDir, SourceFile sourceFile) {
        workDir.writeSource(sourceFile.name(), sourceFile.content());
    }

    @Override
    public abstract DeployResult deploy(long appId);

    @Override
    public void commit(long appId, String message) {
        var workDir = getWorkDir(appId);
        Utils.executeCommand(workDir.path(), "git", "add", "src");
        Utils.executeCommand(workDir.path(), "git", "commit", "-m", "\"" + message + "\"");
    }

    @Override
    public void reset(long appId) {
        Utils.executeCommand(getWorkDir(appId).path(), "git", "reset", "--hard", "HEAD");
    }

    protected WorkDir getWorkDir(long appId) {
        var workDir = WorkDir.from(baseDir, appId);
        if (!workDir.path().toFile().exists()) {
            initWorkDir(workDir, appId);
            Utils.executeCommand(workDir.path(), "git", "init");
        }
        return workDir;
    }

    protected void initWorkDir(WorkDir workDir, long appId) {
        workDir.init();
    }

    @SneakyThrows
    @Override
    public @Nullable String getCode(long appId, String fileName) {
        var workdir = WorkDir.from(baseDir, appId);
        var path = workdir.getSrcPath().resolve(fileName);
        if (path.toFile().isFile())
            return Files.readString(path);
        else
            return null;
    }

    protected abstract BuildResult build(WorkDir workDir);


}
