package org.kiwi.console.genai;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.ContextUtil;
import org.kiwi.console.util.Utils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class AgentCompiler {

    public static final Path baseDir = Path.of("/tmp/kiwiworks");

    private final DeployService deployService;

    public AgentCompiler(DeployService deployService) {
        this.deployService = deployService;
    }

    public DeployResult deploy(long appId, String token, String source) {
        var wd = WorkDir.from(baseDir, appId);
        wd.reset();
        writeSource(wd, source);
        var r = build(wd);
        if (r.successful()) {
            try {
                deploy(appId, token, wd);
            }
            catch (BusinessException e) {
                return new DeployResult(false, e.getMessage());
            }
        }
        return r;
    }

    public @Nullable String getCode(long appId) {
        var workdir = WorkDir.from(baseDir, appId);
        var path = workdir.getSourceFilePath("main.kiwi");
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
        var path = workDir.getSourceFilePath("main.kiwi");
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

    private void deploy(long appId, String token, WorkDir workDir) {
        ContextUtil.setAppId(appId);
        try (var pkgInput = workDir.openTargetInput()) {
            deployService.deploy(appId, token, pkgInput);
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

        public void reset() {
            Utils.clearDirectory(path);
            try {
                Files.createDirectories(getSrcPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private Path getSrcPath() {
           return path.resolve("src");
        }

        public Path getSourceFilePath(String fileName) {
            return getSrcPath().resolve(fileName);
        }

        public InputStream openTargetInput() {
            try {
                return Files.newInputStream(path.resolve("target").resolve("target.mva"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
