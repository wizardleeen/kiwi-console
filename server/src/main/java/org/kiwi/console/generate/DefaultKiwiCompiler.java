package org.kiwi.console.generate;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.util.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DefaultKiwiCompiler extends AbstractCompiler implements KiwiCompiler {

    private final DeployService deployService;
    private static final String ABORT_ERROR_MSG = """
                    Deploy failed. Possible causes:
                    1. Error occurred while running migration functions.
                    2. Trying to remove an enum constant that is being referenced by other objects.
                    3. Trying to remove a class whose instances are being referenced by other objects.
                    """;


    public DefaultKiwiCompiler(Path baseDir, DeployService deployService) {
        super(baseDir);
        this.deployService = deployService;
    }

    @Override
    public DeployResult deploy(long appId, String projectName, boolean deploySource, boolean noBackup) {
        try {
            var deployId = deploy(appId, noBackup, getWorkDir(projectName));
            waitForDeployFinish(appId, deployId);
            return new DeployResult(true, null);
        }
        catch (BusinessException e) {
            return new DeployResult(false, e.getMessage());
        }
    }

    @SneakyThrows
    private void waitForDeployFinish(long appId, String deployId) {
        for(;;) {
            var status = deployService.getDeployStatus(appId, deployId);
            if (status.equals("ABORTED"))
                throw new BusinessException(ErrorCode.DEPLOY_FAILED, ABORT_ERROR_MSG);
            if (status.equals("COMPLETED"))
                return;
            Thread.sleep(1000L);
        }
    }

    public void revert(long appId, String projectName, boolean deploySource) {
        super.revert(appId, projectName, deploySource);
        deployService.revert(appId);
    }

    @SneakyThrows
    public String generateApi(String projectName) {
        var wd = WorkDir.from(baseDir, projectName);
        var version = getVersion(wd);
        var r = Utils.executeCommand(wd.root(), "manul", "gen-api", "--version", Long.toString(version));
        if (r.exitCode() != 0)
            throw new RuntimeException("Failed to generate API: " + r.output());
        return Files.readString(wd.root().resolve("apigen").resolve("api.ts"));
    }

    @SneakyThrows
    protected BuildResult build(WorkDir workDir) {
        var version = getVersion(workDir);
        var command = new ArrayList<>(List.of("manul", "build"));
        if (version > 1)
            command.add("--sense-lint");
        var r = Utils.executeCommand(workDir.root(), command);
        if (r.output().isEmpty())
            return new BuildResult(true, null);
        log.info("Build failed: {}", r.output());
        return new BuildResult(false, r.output());
    }

    @SneakyThrows
    private long getVersion(WorkDir workDir) {
        var versionPath = workDir.root().resolve(".version");
        return Files.exists(versionPath) ? Integer.parseInt(Files.readAllLines(versionPath).getFirst()) : 0;
    }

    private String deploy(long appId, boolean noBackup, WorkDir workDir) {
        try (var pkgInput = workDir.openTargetInput()) {
            return deployService.deploy(appId, noBackup, pkgInput);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        var compiler = new DefaultKiwiCompiler(Path.of("/tmp/kiwi-works"), new MockDeployService());
        var code = "class Foo(var name: string)";
        compiler.deploy(1000037184L, "1000037184", false, false);
        var deployedCode = compiler.getCode("1000037184", Constants.MAIN_KIWI);
        Utils.require(code.equals(deployedCode));
    }

}
