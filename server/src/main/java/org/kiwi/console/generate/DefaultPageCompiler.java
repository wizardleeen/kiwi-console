package org.kiwi.console.generate;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.util.Utils;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@Slf4j
public class DefaultPageCompiler extends AbstractCompiler implements PageCompiler {

    public DefaultPageCompiler(Path baseDir) {
        super(baseDir);
    }

    @Override
    public DeployResult deploy(long appId, String projectName, boolean deploySource, boolean noBackup) {
        var cmd = new ArrayList<String>();
        cmd.add("deploy-page");
        if (deploySource)
            cmd.add("-s");
        var r = Utils.executeCommand(getWorkDir(projectName).root(), cmd.toArray(String[]::new));
        if (r.exitCode() != 0)
            return new DeployResult(false, r.output());
        return new DeployResult(true, null);
    }

    @SneakyThrows
    @Override
    protected void initWorkDir(WorkDir workDir, String projectName) {
        var r1 = Utils.executeCommand(workDir.root(), "pnpm", "install");
        if (r1.exitCode() != 0)
            throw new RuntimeException("Failed to run `pnpm install`: " + r1.output());
        var envFilePath = workDir.getSrcPath().resolve("env.ts");
        Files.writeString(envFilePath, "export const APP_ID = " + projectName);
        Utils.executeCommand(workDir.root(), "git", "add", "src/env.ts");
        Utils.executeCommand(workDir.root(), "git", "commit", "-m", "\"Add env.ts\"");
    }

    protected BuildResult build(WorkDir workDir) {
        Utils.CommandResult r;
        r = Utils.executeCommand(workDir.root(), "pnpm", "run", "build");
        if (r.exitCode() == 0)
            return new BuildResult(true, null);
        log.info("Build failed: {}", r.output());
        return new BuildResult(false, r.output());
    }

    public static void main(String[] args) {
        var compiler = new DefaultPageCompiler(Path.of("/tmp/page-works"));
        compiler.deploy( 1, "1", false, false);
    }

    @SneakyThrows
    @Override
    public @Nullable Path getSourceMapPath(String projectName) {
        var wd = getWorkDir(projectName);
        var assetsDir = wd.getSrcPath().resolve("dist").resolve("assets");
        if (Files.isDirectory(assetsDir)) {
            try (var s = Files.list(assetsDir)) {
                var opt = s.filter(f -> f.getFileName().toString().endsWith(".js.map")).findAny();
                if (opt.isPresent())
                    return opt.get();
            }
        }
        return null;
    }
}
