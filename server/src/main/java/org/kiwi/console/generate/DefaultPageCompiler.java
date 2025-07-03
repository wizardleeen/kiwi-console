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
    public DeployResult deploy(long appId, String token) {
        return new DeployResult(true, null);
    }

    @SneakyThrows
    @Override
    protected void initWorkDir(WorkDir workDir, long appId) {
        if(!workDir.path().toFile().exists()) {
            var r = Utils.executeCommand(workDir.path().getParent(), "cp", "-r", "template",
                    workDir.path().getFileName().toString());
            if (r.exitCode() != 0)
                throw new RuntimeException("Failed to initialized frontend workspace");
            var r1 = Utils.executeCommand(workDir.path(), "npm", "install");
            if (r1.exitCode() != 0)
                throw new RuntimeException("Failed to run `npm install`: " + r1.output());
            var envFilePath = workDir.getSrcPath().resolve("env.ts");
            Files.writeString(envFilePath, "export const APP_ID = " + appId);
        }
    }
    protected BuildResult build(WorkDir workDir) {
        Utils.CommandResult r;
        r = Utils.executeCommand(workDir.path(), "npm", "run", "build");
        if (r.exitCode() == 0)
            return new BuildResult(true, null);
        log.info("Build failed: {}", r.output());
        return new BuildResult(false, r.output());
    }

    public static void main(String[] args) {
        var compiler = new DefaultPageCompiler(Path.of("/tmp/page-works"));
        compiler.deploy(1, "");
    }

}
