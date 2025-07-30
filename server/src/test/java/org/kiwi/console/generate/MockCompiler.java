package org.kiwi.console.generate;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MockCompiler implements KiwiCompiler, PageCompiler {

    private Map<Long, Map<String, String>> working = new HashMap<>();
    private final List<Commit> commits = new ArrayList<>();

    @Override
    public DeployResult run(long appId, List<SourceFile> sourceFiles, List<Path> removedFiles) {
        var files = getWorkdir(appId);
        sourceFiles.forEach(f -> files.put(f.path().toString(), f.content()));
        for (Path removedFile : removedFiles) {
            files.remove(removedFile.toString());
        }
        if (sourceFiles.stream().anyMatch(f -> f.content().contains("Error")))
            return new DeployResult(false, "Compilation failed.");
        else
            return new DeployResult(true, null);
    }

    private Map<String, String> getWorkdir(long appId) {
        var wd = working.get(appId);
        if (wd == null) {
            wd = new HashMap<>();
            working.put(appId, wd);
            initWorkdir(wd);
        }
        return wd;
    }

    protected void initWorkdir(Map<String, String> workdir) {
    }

    @Override
    public List<SourceFile> getSourceFiles(long appId) {
        var files = getWorkdir(appId);
        var sourceFiles = new ArrayList<SourceFile>();
        files.forEach((path, content) -> sourceFiles.add(new SourceFile(Path.of(path), content)));
        return sourceFiles;
    }

    @Override
    public void addFile(long appId, SourceFile file) {
        getWorkdir(appId).put(file.path().toString(), file.content());
    }

    @Override
    public DeployResult deploy(long appId) {
        return new DeployResult(true, "");
    }

    @Override
    public void commit(long appId, String message) {
        commits.add(new Commit(copyMap(working.get(appId)), message));
    }

    @Nullable
    public String getLastCommitMessage() {
        return commits.isEmpty() ? null : commits.getLast().message();
    }

    @Override
    public void reset(long appId, String templateRepo) {
        if (commits.isEmpty())
            working = new HashMap<>();
        else
            working.put(appId, copyMap(commits.getLast().map()));
    }

    private Map<String, String> copyMap(Map<String, String> map) {
        return new HashMap<>(map);
    }

    @Override
    public void delete(long appId) {
        working.remove(appId);
    }

    @Override
    public void revert(long appId) {
        if (commits.isEmpty())
            throw new IllegalStateException("No commits to revert to.");
        if (commits.size() == 1)
            working.remove(appId);
        else
            working.put(appId, copyMap(commits.get(commits.size() - 2).map()));
    }

    @Override
    public String generateApi(long appId) {
        return "api";
    }

    public String getCode(long appId, String path) {
        return getWorkdir(appId).get(path);
    }

    private record Commit(Map<String, String> map, String message) {}

}
