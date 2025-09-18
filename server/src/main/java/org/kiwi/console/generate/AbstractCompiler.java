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
    public DeployResult run(long appId, String projectName, List<SourceFile> sourceFiles, List<Path> removedFiles, boolean deploySource, boolean noBackup) {
        var workDir = getWorkDir(projectName);
        for (Path removedFile : removedFiles) {
            if (workDir.exist(removedFile))
                workDir.removeFile(removedFile);
        }
        sourceFiles.forEach(f -> writeSourceFile(workDir, f));
        var r = build(workDir);
        if (r.successful())
            return deploy(appId, projectName, deploySource, noBackup);
        else
            return new DeployResult(false, r.message());
    }

    @Override
    public void addFile(String projectName, SourceFile file) {
        writeSourceFile(getWorkDir(projectName), file);
    }

    @SneakyThrows
    private void writeSourceFile(WorkDir workDir, SourceFile sourceFile) {
        workDir.writeSource(sourceFile.path(), sourceFile.content());
    }

    @Override
    public List<SourceFile> getSourceFiles(String projectName) {
        return getWorkDir(projectName).getAllSourceFiles();
    }

    @Override
    public abstract DeployResult deploy(long appId, String projectName, boolean deploySource, boolean noBackup);

    @Override
    public void commit(String projectName, String message) {
        var workDir = getWorkDir(projectName);
        Utils.executeCommand(workDir.root(), "git", "add", ".");
        Utils.executeCommand(workDir.root(), "git", "commit", "-m", "\"" + message + "\"");
    }

    public void revert(long appId, String projectName, boolean deploySource) {
        var workDir = getWorkDir(projectName);
        Utils.executeCommand(workDir.root(), "git", "revert", "HEAD");
    }

    @Override
    public void reset(String projectName, String templateRepo, String branch) {
        var dir = baseDir.resolve(projectName);
        if (dir.toFile().exists()) {
            Utils.executeCommand(getWorkDir(projectName).root(), "git", "reset", "--hard", "HEAD");
            Utils.executeCommand(getWorkDir(projectName).root(), "git", "clean", "-fdx", "--exclude=node_modules", "--exclude=dist");
        } else {
            Utils.executeCommand(baseDir, "git", "clone", templateRepo, projectName);
            var workdir = getWorkDir(projectName);
            Utils.executeCommand(workdir.root(), "git", "checkout", branch);
            initWorkDir(workdir, projectName);
        }
    }

    protected WorkDir getWorkDir(String projectName) {
        return WorkDir.from(baseDir, projectName);
    }

    protected void initWorkDir(WorkDir workDir, String projectName) {
    }

    @SneakyThrows
    public @Nullable String getCode(String projectName, String fileName) {
        var workdir = WorkDir.from(baseDir, projectName);
        var path = workdir.getSrcPath().resolve(fileName);
        if (path.toFile().isFile())
            return Files.readString(path);
        else
            return null;
    }

    public void delete(String projectName) {
        var workDir = WorkDir.from(baseDir, projectName);
        var r = Utils.executeCommand(Path.of("."), "rm", "-rf", workDir.root().toString());
        if (r.exitCode() != 0)
            log.warn("Failed to delete work directory for app {}: {}", projectName, r.output());
    }

    protected abstract BuildResult build(WorkDir workDir);


}
