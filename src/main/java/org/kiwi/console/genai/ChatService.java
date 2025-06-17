package org.kiwi.console.genai;

import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.genai.rest.dto.ChatRequest;
import org.kiwi.console.genai.rest.dto.ChatResponse;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.Utils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
@Component
public class ChatService {

    private final Agent agent;
    private final String createPrompt;
    private final String fixPrompt;
    private final String updatePrompt;
    private final AgentCompiler agentCompiler;

    public ChatService(Agent agent, AgentCompiler agentCompiler) {
        this.agent = agent;
        this.agentCompiler = agentCompiler;
        createPrompt = loadPrompt("/prompt/create.md");
        fixPrompt = loadPrompt("/prompt/fix.md");
        updatePrompt = loadPrompt("/prompt/update.md");
    }

    private String loadPrompt(String file) {
        try (var input = ChatService.class.getResourceAsStream(file)) {
            return new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ChatResponse chat(ChatRequest request, String token) {
        var chat = agent.createChat();
        var existingCode = agentCompiler.getCode(request.appId());
        String text;
        if (existingCode != null)
            text = buildUpdateText(request.content(), existingCode);
        else
            text = buildCreateText(request);
        log.info("Initial prompt: {}", text);
        var code = removeMarkdownTags(chat.send(text));
        log.info("Generated code:");
        log.info("{}", code);
        var r = agentCompiler.deploy(request.appId(), token, code);
        if (r.successful())
            return new ChatResponse(code);
        for (int i = 0; i < 5; i++) {
            var fixPrompt = buildFixPrompt(r.output());
            log.info("Trying to fix error with prompt: {}", fixPrompt);
            code = removeMarkdownTags(chat.send(fixPrompt));
            log.info("Generated code (Retry #{}):\n{}", i + 1, code);
            r = agentCompiler.deploy(request.appId(), token, code);
            if (r.successful())
                return new ChatResponse(code);
        }
        throw new BusinessException(ErrorCode.CODE_GENERATION_FAILED);
    }

    private String buildCreateText(ChatRequest request) {
        return createPrompt + request.content();
    }

    private String buildUpdateText(String content, String code) {
        return updatePrompt + content + "\nHere is the existing code:\n" + code;
    }

    private String buildFixPrompt(String buildOutput) {
        return fixPrompt + buildOutput;
    }

    public static String removeMarkdownTags(String text) {
        if (text.startsWith("```kiwi")) {
            var endIdx = text.lastIndexOf("```");
            if (endIdx >= 7)
                return text.substring(7, endIdx);
        }
        return text;
    }

    public static void main(String[] args) throws IOException {
        Utils.clearDirectory("/tmp/kiwiworks/1000012000");
        var apikeyPath = "/Users/leen/develop/gemini/apikey";
        var apikey = Files.readString(Path.of(apikeyPath));
        var chatService = new ChatService(
                new GeminiAgent(apikey),
                new AgentCompiler(new DeployClient("http://localhost:8080"))
        );
        chatService.chat(new ChatRequest(
                1000012000L,
                "Create a todo list application"
        ), "f1adbd81-51f3-4398-9c23-d611e1ca4103");
    }

}
