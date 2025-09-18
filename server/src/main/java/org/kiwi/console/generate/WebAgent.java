package org.kiwi.console.generate;

import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.kiwi.Tech;
import org.kiwi.console.patch.PatchReader;

import java.nio.file.Path;
import java.util.List;

import static org.kiwi.console.util.Constants.API_TS;

@Slf4j
public class WebAgent extends CodeAgent {

    private final PageCompiler compiler;

    public WebAgent(PageCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public void generate(GenerationRequest request) {
        if (request.getApiFiles().isEmpty())
            throw new IllegalArgumentException("API source code is required for web page generation");
        var projName = request.getProjectName();
        var apiSource = request.getApiFiles().getFirst().content();
        var chat = request.getModel().createChat(request.isOutputThinking());
        var existingFiles = compiler.getSourceFiles(projName);
        String prompt;
        var existingSource = PatchReader.buildCode(existingFiles);
        if (existingFiles.stream().noneMatch(f -> f.path().toString().equals(API_TS)))
            prompt = buildPageCreatePrompt(request.getCreateTemplate(), request.getAppName(), request.getRequirement(), existingSource, apiSource);
        else
            prompt = buildPageUpdatePrompt(request.getUpdateTemplate(), request.getRequirement(), existingSource, apiSource, request.getSuggestion());
        log.info("Page generation prompt:\n{}", prompt);
        compiler.addFile(projName, new SourceFile(Path.of(API_TS), apiSource));
        request.getListener().onAttemptStart();
        var r = generateCode(chat, prompt, request);
        if (!r.successful()) {
            request.getListener().onAttemptFailure(r.output());
            fix(r.output(), chat, request);
        } else
            request.getListener().onAttemptSuccess();
        log.info("Pages generated successfully");
//        compiler.commit(appId, generatePagesCommitMsg(request.getRequirement()));
//        log.info("Page source code committed");
    }

    private String buildPageUpdatePrompt(String template, String requirement, String existingCode, String apiSource, String suggestion) {
        return Format.format(template, requirement, suggestion, existingCode, apiSource);
    }

    private String buildPageCreatePrompt(String template, String appName, String requirement, String existingSource, String apiSource) {
        return Format.format(template, appName, requirement, existingSource, apiSource);
    }

    private String generatePagesCommitMsg(String requirement) {
        return requirement;
    }

    @Override
    protected PageCompiler getCompiler() {
        return compiler;
    }

    @Override
    public Tech getTech() {
        return Tech.WEB;
    }

    @Override
    public List<SourceFile> generateApiSources(String projectName) {
        return List.of();
    }

}
