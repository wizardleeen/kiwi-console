package org.kiwi.console.generate;

import jakarta.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.util.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public abstract class AbstractCompiler implements Compiler {

    protected final Path baseDir;

    public AbstractCompiler(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public DeployResult run(long appId, List<SourceFile> sourceFiles, List<Path> removedFiles) {
        var workDir = getWorkDir(appId);
        for (Path removedFile : removedFiles) {
            if (workDir.exist(removedFile))
                workDir.removeFile(removedFile);
        }
        sourceFiles.forEach(f -> writeSourceFile(workDir, f));
        var r = build(workDir);
        if (r.successful())
            return deploy(appId);
        else
            return new DeployResult(false, r.message());
    }

    @Override
    public void addFile(long appId, SourceFile file) {
        writeSourceFile(getWorkDir(appId), file);
    }

    @SneakyThrows
    private void writeSourceFile(WorkDir workDir, SourceFile sourceFile) {
        workDir.writeSource(sourceFile.path(), sourceFile.content());
    }

    @Override
    public List<SourceFile> getSourceFiles(long appId) {
        return getWorkDir(appId).getAllSourceFiles();
    }

    @Override
    public abstract DeployResult deploy(long appId);

    @Override
    public void commit(long appId, String message) {
        var workDir = getWorkDir(appId);
        Utils.executeCommand(workDir.root(), "git", "add", ".");
        Utils.executeCommand(workDir.root(), "git", "commit", "-m", "\"" + message + "\"");
    }

    public void revert(long appId) {
        var workDir = getWorkDir(appId);
        Utils.executeCommand(workDir.root(), "git", "revert", "HEAD");
    }

    @Override
    public void reset(long appId, String templateRepo) {
        var dir = baseDir.resolve(Long.toString(appId));
        if (dir.toFile().exists()) {
            Utils.executeCommand(getWorkDir(appId).root(), "git", "reset", "--hard", "HEAD");
            Utils.executeCommand(getWorkDir(appId).root(), "git", "clean", "-fdx", "--exclude=node_modules");
        } else {
            Utils.executeCommand(baseDir, "git", "clone", templateRepo, Long.toString(appId));
            initWorkDir(getWorkDir(appId), appId);
        }
    }

    protected WorkDir getWorkDir(long appId) {
        return WorkDir.from(baseDir, appId);
    }

    protected void initWorkDir(WorkDir workDir, long appId) {
    }

    @SneakyThrows
    public @Nullable String getCode(long appId, String fileName) {
        var workdir = WorkDir.from(baseDir, appId);
        var path = workdir.getSrcPath().resolve(fileName);
        if (path.toFile().isFile())
            return Files.readString(path);
        else
            return null;
    }

    public void delete(long appId) {
        var workDir = WorkDir.from(baseDir, appId);
        var r = Utils.executeCommand(Path.of("."), "rm", "-rf", workDir.root().toString());
        if (r.exitCode() != 0)
            log.warn("Failed to delete work directory for app {}: {}", appId, r.output());
    }

    protected abstract BuildResult build(WorkDir workDir);


}
