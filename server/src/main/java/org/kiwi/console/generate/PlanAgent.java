package org.kiwi.console.generate;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.util.Utils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

@Slf4j
public class PlanAgent {

    public Plan plan(PlanRequest request) {
        if (request.first())
            return planForCreate(request);
        else
            return planForUpdate(request);
    }

    @SneakyThrows
    private Plan planForCreate(PlanRequest request) {
        var prompt = createInitialPlanPrompt(request.createTemplate(), request.requirement());
        log.info("Plan prompt\n{}", prompt);
        var text = Models.generateContent(request.model(), prompt, request.attachments(), request.abortController());
        var reader = new BufferedReader(new StringReader(text));
        reader.readLine();
        var appName = reader.readLine();
        if (appName == null)
            throw new AgentException("Failed to parse plan: "+ text);
        return new Plan(appName, List.of());
    }

    private Plan planForUpdate(PlanRequest request) {
        var prompt = createUpdatePlanPrompt(request.updateTemplate(), request.requirement(), request.modules());
        log.info("Plan prompt\n{}", prompt);
        var text = CodeSanitizer.sanitizeCode(Models.generateContent(request.model(), prompt, request.attachments(), request.abortController()));
        try {
            return Utils.readJSONString(text, Plan.class);
        } catch (Exception e) {
            throw new AgentException("Failed to parse plan: " + text, e);
        }
    }

    private String createInitialPlanPrompt(String createPlanTemplate, String requirement) {
        return Format.format(createPlanTemplate, requirement);
    }

    private String createUpdatePlanPrompt(String updatePlanTemplate, String requirement, List<ModuleInfo> modules) {
        return Format.format(updatePlanTemplate, requirement, buildCodes(modules));
    }

    private String buildCodes(List<ModuleInfo> modules) {
        var sb = new StringBuilder();
        modules.forEach(mod ->
            sb.append("#### ").append(mod.name()).append("\n\n")
                    .append("**Module Type: **").append(mod.tech()).append("\n\n")
                    .append("**Module Description**\n").append(mod.description()).append("\n\n")
                    .append("**Module Code**\n\n").append(mod.code()).append("\n\n")
        );
        return sb.toString();
    }



}
