package org.kiwi.console.generate;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.util.Utils;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class DefaultPageCompiler extends AbstractCompiler implements PageCompiler {

    public DefaultPageCompiler(Path baseDir) {
        super(baseDir);
    }

    @Override
    public DeployResult deploy(long appId) {
        var r = Utils.executeCommand(getWorkDir(appId).root(), "deploy-page");
        if (r.exitCode() != 0)
            return new DeployResult(false, r.output());
        return new DeployResult(true, null);
    }

    @SneakyThrows
    @Override
    protected void initWorkDir(WorkDir workDir, long appId) {
        var r1 = Utils.executeCommand(workDir.root(), "pnpm", "install");
        if (r1.exitCode() != 0)
            throw new RuntimeException("Failed to run `pnpm install`: " + r1.output());
        var envFilePath = workDir.getSrcPath().resolve("env.ts");
        Files.writeString(envFilePath, "export const APP_ID = " + appId);
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
        compiler.deploy(1);
    }

}
