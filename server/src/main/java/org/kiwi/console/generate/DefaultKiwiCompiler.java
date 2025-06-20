package org.kiwi.console.generate;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.ContextUtil;
import org.kiwi.console.util.Result;
import org.kiwi.console.util.Utils;

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
    public DeployResult deploy(long appId, String token) {
        var r1 = deploy(appId, token, getWorkDir(appId));
        if (r1.isSuccessful())
            return new DeployResult(true, "");
        else
            return new DeployResult(false, r1.message());
    }

    @SneakyThrows
    public String generateApi(long appId) {
        var wd = WorkDir.from(baseDir, appId);
        var r = Utils.executeCommand(wd.path(), "kiwi", "gen-api");
        if (r.exitCode() != 0)
            throw new RuntimeException("Failed to generate API: " + r.output());
        return Files.readString(wd.path().resolve("apigen").resolve("api.ts"));
    }

    protected BuildResult build(WorkDir workDir) {
        Utils.CommandResult r;
        r = Utils.executeCommand(workDir.path(), "kiwi", "build");
        if (r.output().isEmpty())
            return new BuildResult(true, null);
        log.info("Build failed: {}", r.output());
        return new BuildResult(false, r.output());
    }

    private Result<String> deploy(long appId, String token, WorkDir workDir) {
        ContextUtil.setAppId(appId);
        try (var pkgInput = workDir.openTargetInput()) {
            return deployService.deploy(appId, token, pkgInput);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            ContextUtil.setAppId(Constants.PLATFORM_APP_ID);
        }
    }

    public static void main(String[] args) {
        var compiler = new DefaultKiwiCompiler(Path.of("/tmp/kiwi-works"), new MockDeployService());
        var code = "class Foo(var name: string)";
        compiler.deploy(1000015059L, "");
        var deployedCode = compiler.getCode(1000015059L, Constants.MAIN_KIWI);
        Utils.require(code.equals(deployedCode));
    }

}
