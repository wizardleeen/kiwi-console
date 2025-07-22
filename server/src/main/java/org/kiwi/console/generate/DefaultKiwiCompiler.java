package org.kiwi.console.generate;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.util.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class DefaultKiwiCompiler extends AbstractCompiler implements KiwiCompiler {

    private final DeployService deployService;

    public DefaultKiwiCompiler(Path baseDir, DeployService deployService) {
        super(baseDir);
        this.deployService = deployService;
    }

    @Override
    public DeployResult deploy(long appId) {
        try {
            deploy(appId, getWorkDir(appId));
            return new DeployResult(true, null);
        }
        catch (BusinessException e) {
            return new DeployResult(false, e.getMessage());
        }
    }

    @SneakyThrows
    public String generateApi(long appId) {
        var wd = WorkDir.from(baseDir, appId);
        var versionPath = wd.root().resolve(".version");
        var version = Files.exists(versionPath) ? Integer.parseInt(Files.readString(versionPath)) : 0;
        var r = version > 0 ?
            Utils.executeCommand(wd.root(), "kiwi", "gen-api", "--return-full-object"):
            Utils.executeCommand(wd.root(), "kiwi", "gen-api");
        if (r.exitCode() != 0)
            throw new RuntimeException("Failed to generate API: " + r.output());
        return Files.readString(wd.root().resolve("apigen").resolve("api.ts"));
    }

    protected BuildResult build(WorkDir workDir) {
        Utils.CommandResult r;
        r = Utils.executeCommand(workDir.root(), "kiwi", "build");
        if (r.output().isEmpty())
            return new BuildResult(true, null);
        log.info("Build failed: {}", r.output());
        return new BuildResult(false, r.output());
    }

    private void deploy(long appId, WorkDir workDir) {
        try (var pkgInput = workDir.openTargetInput()) {
            deployService.deploy(appId, pkgInput);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        var compiler = new DefaultKiwiCompiler(Path.of("/tmp/kiwi-works"), new MockDeployService());
        var code = "class Foo(var name: string)";
        compiler.deploy(1000037184L);
        var deployedCode = compiler.getCode(1000037184L, Constants.MAIN_KIWI);
        Utils.require(code.equals(deployedCode));
    }

}
