package org.kiwi.console.generate;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MockCompiler implements KiwiCompiler, PageCompiler {

    private Map<String, Map<String, String>> working = new HashMap<>();
    private final List<Commit> commits = new ArrayList<>();

    @Override
    public DeployResult run(long appId, String projectName, List<SourceFile> sourceFiles, List<Path> removedFiles, boolean deploySource, boolean noBackup) {
        var files = getWorkdir(projectName);
        sourceFiles.forEach(f -> files.put(f.path().toString(), f.content()));
        for (Path removedFile : removedFiles) {
            files.remove(removedFile.toString());
        }
        if (sourceFiles.stream().anyMatch(f -> f.content().contains("Error")))
            return new DeployResult(false, "Compilation failed.");
        else
            return new DeployResult(true, null);
    }

    private Map<String, String> getWorkdir(String projectName) {
        var wd = working.get(projectName);
        if (wd == null) {
            wd = new HashMap<>();
            working.put(projectName, wd);
            initWorkdir(wd);
        }
        return wd;
    }

    protected void initWorkdir(Map<String, String> workdir) {
    }

    @Override
    public List<SourceFile> getSourceFiles(String projectName) {
        var files = getWorkdir(projectName);
        var sourceFiles = new ArrayList<SourceFile>();
        files.forEach((path, content) -> sourceFiles.add(new SourceFile(Path.of(path), content)));
        return sourceFiles;
    }

    @Override
    public void addFile(String projectName, SourceFile file) {
        getWorkdir(projectName).put(file.path().toString(), file.content());
    }

    @Override
    public DeployResult deploy(long appId, String projectName, boolean deploySource, boolean noBackup) {
        return new DeployResult(true, "");
    }

    @Override
    public void commit(String projectName, String message) {
        commits.add(new Commit(copyMap(working.get(projectName)), message));
    }

    @Nullable
    public String getLastCommitMessage() {
        return commits.isEmpty() ? null : commits.getLast().message();
    }

    @Override
    public void reset(String projectName, String templateRepo, String branch) {
        if (commits.isEmpty())
            working = new HashMap<>();
        else
            working.put(projectName, copyMap(commits.getLast().map()));
    }

    boolean isEmpty() {
        return commits.isEmpty();
    }

    private Map<String, String> copyMap(Map<String, String> map) {
        return new HashMap<>(map);
    }

    @Override
    public void delete(String projectName) {
        working.remove(projectName);
    }

    @Override
    public void revert(long appId, String projectName, boolean deploySource) {
        if (commits.isEmpty())
            throw new IllegalStateException("No commits to revert to.");
        if (commits.size() == 1)
            working.remove(projectName);
        else
            working.put(projectName, copyMap(commits.get(commits.size() - 2).map()));
    }

    @Override
    public String generateApi(String projectName) {
        return "api";
    }

    public String getCode(String projectName, String path) {
        return getWorkdir(projectName).get(path);
    }

    @Override
    public @Nullable Path getSourceMapPath(String projectName) {
        return null;
    }

    private record Commit(Map<String, String> map, String message) {}

}
