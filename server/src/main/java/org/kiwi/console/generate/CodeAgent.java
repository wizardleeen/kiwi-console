package org.kiwi.console.generate;

import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.kiwi.Tech;
import org.kiwi.console.patch.PatchReader;
import org.kiwi.console.util.Utils;

import java.util.List;

@Slf4j
public abstract class CodeAgent {

    public abstract void generate(GenerationRequest request);

    public void deploy(long appId, String projectName, boolean deploySource, boolean noBackup) {
        getCompiler().deploy(appId, projectName, deploySource, noBackup);
    }

    protected DeployResult generateCode(Chat chat, String prompt, GenerationRequest request) {
        try {
            var patch = new PatchReader(Models.generateContent(chat, prompt, request.getAttachments(), request.getAbortController())).read();
            return getCompiler().run(request.getAppId(), request.getProjectName(), patch.addedFiles(), patch.removedFiles(), request.isDeploySource(), request.isNoBackup());
        } catch (MalformedHunkException e) {
            return new DeployResult(false, e.getMessage());
        }
    }

    protected void fix(String error,
                     Chat chat,
                     GenerationRequest request) {
        for (int i = 0; i < 5; i++) {
            request.getListener().onAttemptStart();
            var fixPrompt = Format.format(request.getFixTemplate(), error);
            log.info("Fix prompt:\n{}", fixPrompt);
            var r = generateCode(chat, fixPrompt, request);
            if (r.successful()) {
                request.getListener().onAttemptSuccess();
                return;
            }
            error = r.output();
            request.getListener().onAttemptFailure(error);
        }
        throw new RuntimeException("Failed to fix compilation errors after 5 attempts: " + error);
    }

    protected abstract Compiler getCompiler();

    public void commit(String projectName, String message) {
        getCompiler().commit(projectName, message);
    }

    public void reset(String projectName, String templateRepo, String branch) {
        getCompiler().reset(projectName, templateRepo, branch);
    }

    public void revert(long appId, String projectName, boolean deploySource) {
        getCompiler().revert(appId, projectName, deploySource);
    }

    public List<SourceFile> getSourceFiles(String projectName) {
        return getCompiler().getSourceFiles(projectName);
    }

    public String getCode(String projectName, String path) {
        return Utils.findRequired(getCompiler().getSourceFiles(projectName), f -> f.path().toString().equals(path)).content();
    }

    public abstract Tech getTech();

    public abstract List<SourceFile> generateApiSources(String projectName);

    public void delete(String projectName) {
        getCompiler().delete(projectName);
    }
}
