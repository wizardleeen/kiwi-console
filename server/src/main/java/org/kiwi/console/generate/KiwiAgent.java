package org.kiwi.console.generate;

import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.kiwi.Tech;
import org.kiwi.console.patch.PatchReader;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public class KiwiAgent extends CodeAgent {

    private final KiwiCompiler compiler;

    public KiwiAgent(KiwiCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public void generate(GenerationRequest request) {
        String prompt;
        request.getListener().onAttemptStart();
        var existingFiles = compiler.getSourceFiles(request.getProjectName());
        if (existingFiles.isEmpty())
            prompt = buildCreatePrompt(request.getCreateTemplate(), request.getRequirement());
        else
            prompt = buildUpdatePrompt(request.getUpdateTemplate(), request.getRequirement(), request.getSuggestion(), PatchReader.buildCode(existingFiles));
        log.info("Kiwi generation prompt: \n{}", prompt);
        var chat = request.getModel().createChat(false);
        var r = generateCode(chat, prompt, request);
//        if (existingFiles == null) {
//            var appName = extractAppName(resp);
//            if (appName != null)
//                updateAppName(task.exchange.getAppId(), appName);
//        }
        if (!r.successful()) {
            request.getListener().onAttemptFailure(r.output());
            fix(r.output(), chat, request);
        } else
            request.getListener().onAttemptSuccess();
        log.info("Kiwi deployed successfully");
//        compiler.commit(request.getProjectName(), generateKIwiCommitMsg(request.getRequirement()));
//        log.info("Kiwi source code committed");
    }

    private String generateKIwiCommitMsg(String requirement) {
        return requirement;
    }


    private String buildCreatePrompt(String template, String requirement) {
        return Format.format(template, requirement);
    }

    private String buildUpdatePrompt(String template, String requirement, String suggestion, String code) {
        return Format.format(template, requirement, suggestion != null ? suggestion : "N/A", code);
    }

    @Override
    protected Compiler getCompiler() {
        return compiler;
    }

    @Override
    public Tech getTech() {
        return Tech.KIWI;
    }

    @Override
    public List<SourceFile> generateApiSources(String projectName) {
        return List.of(new SourceFile(Path.of("src/api.ts"), compiler.generateApi(projectName)));
    }

    public String generateApi(String projectName) {
        return compiler.generateApi(projectName);
    }
}
