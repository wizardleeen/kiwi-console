package org.kiwi.console.genai;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.util.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class AgentCompilerImpl implements AgentCompiler {

    private static final Path baseDir = Path.of("/tmp/kiwiworks");

    private final DeployService deployService;

    public AgentCompilerImpl(DeployService deployService) {
        this.deployService = deployService;
    }

    @Override
    public DeployResult deploy(long appId, String token, String source) {
        var wd = WorkDir.from(baseDir, appId);
        wd.init();
        writeSource(wd, source);
        var r = build(wd);
        if (r.successful()) {
            var r1 = deploy(appId, token, wd);
            if (r1.isSuccessful()) {
                writeStableSource(wd, source);
                return new DeployResult(true, "");
            } else
                return new DeployResult(false, r1.message());
        }
        return r;
    }

    @Override
    public @Nullable String getCode(long appId) {
        var workdir = WorkDir.from(baseDir, appId);
        var path = workdir.getStableSrcPath().resolve("main.kiwi");
        if (path.toFile().isFile()) {
            try {
                return Files.readString(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else
            return null;
    }

    private void writeSource(WorkDir workDir, String source) {
        var path = workDir.getSrcPath().resolve("main.kiwi");
        try {
            Files.writeString(path, source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeStableSource(WorkDir workDir, String source) {
        var path = workDir.getStableSrcPath().resolve("main.kiwi");
        try {
            Files.writeString(path, source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DeployResult build(WorkDir workDir) {
        Utils.CommandResult r;
        r = Utils.executeCommand(workDir.path, "kiwi", "build");
        if (r.output().isEmpty())
            return new DeployResult(true, null);
        log.info("Build failed: {}", r.output());
        return new DeployResult(false, r.output());
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

    private record WorkDir(Path path) {

        public static WorkDir from(Path baseDir, long appId) {
            return new WorkDir(baseDir.resolve(Long.toString(appId)));
        }

        public void init() {
            try {
                if (!path.toFile().exists()) {
                    Files.createDirectory(path);
                    Files.createDirectory(getSrcPath());
                    Files.createDirectory(getStableSrcPath());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private Path getSrcPath() {
           return path.resolve("src");
        }

        private Path getStableSrcPath() {
            return path.resolve("stable-src");
        }

        public InputStream openTargetInput() {
            try {
                return Files.newInputStream(path.resolve("target").resolve("target.mva"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static void main(String[] args) {
        var compiler = new AgentCompilerImpl(new MockDeployService());
        var code = "class Foo(var name: string)";
        compiler.deploy(1000015059L, "", code);
        var deployedCode = compiler.getCode(1000015059L);
        Utils.require(code.equals(deployedCode));
    }

}
