package org.kiwi.console.genai;

import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.genai.rest.dto.GenerateRequest;
import org.kiwi.console.generate.*;
import org.kiwi.console.patch.PatchReader;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.Utils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class AigcService {

    private final Model model;
    private final String createPrompt;
    private final String fixPrompt;
    private final String updatePrompt;
    private final KiwiCompiler compiler;

    public AigcService(GeminiModel model, KiwiCompiler compiler) {
        this.model = model;
        this.compiler = compiler;
        createPrompt = loadPrompt("/prompt/kiwi-create.md");
        fixPrompt = loadPrompt("/prompt/kiwi-fix.md");
        updatePrompt = loadPrompt("/prompt/kiwi-update.md");
    }

    private String loadPrompt(String file) {
        try (var input = AigcService.class.getResourceAsStream(file)) {
            return new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void generate(GenerateRequest request) {
        compiler.reset(request.appId() + "", Constants.KIWI_TEMPLATE_REPO, "main");
        var chat = model.createChat(false);
        var existingCode = compiler.getSourceFiles(request.appId() + "");
        String text;
        if (!existingCode.isEmpty())
            text = buildUpdateText(request.prompt(), PatchReader.buildCode(existingCode));
        else
            text = buildCreateText(request);
        log.info("Initial prompt:\n{}", text);
        var code = removeMarkdownTags(generateContent(chat, text));
        log.info("Generated code:\n{}", code);
        var r = deploy(request.appId(), code);
        if (r.successful()) {
            log.info("Deployed successfully");
            return;
        }
        for (int i = 0; i < 5; i++) {
            var fixPrompt = buildFixPrompt(code, r.output());
            log.info("Trying to fix error with prompt:\n{}", fixPrompt);
            code = removeMarkdownTags(generateContent(chat, fixPrompt)); //PatchApply.apply(code, removeMarkdownTags(generateContent(chat, fixPrompt)));
            log.info("Generated code (Retry #{}):\n{}", i + 1, code);
            r = deploy(request.appId(), code);
            if (r.successful()) {
                log.info("Deployed successfully");
                return;
            }
        }
        throw new BusinessException(ErrorCode.CODE_GENERATION_FAILED);
    }

    private DeployResult deploy(long appId, String code) {
        var r = compiler.run(appId, appId + "", new PatchReader(code).read().addedFiles(), List.of(), false, false);
        if (r.successful())
            compiler.commit(appId + "", "commit");
        return r;
    }

    private String generateContent(Chat chat, String prompt) {
        var buf = new StringBuilder();
        chat.send(prompt, List.of(), new ChatStreamListener() {
            @Override
            public void onThought(String thoughtChunk) {
                log.info(thoughtChunk);
            }

            @Override
            public void onContent(String contentChunk) {
                log.info(contentChunk);
                buf.append(contentChunk);
            }
        }, () -> false);
        return buf.toString();
    }

    private String buildCreateText(GenerateRequest request) {
        return Format.format(createPrompt, request.prompt());
    }

    private String buildUpdateText(String content, String code) {
        return Format.format(updatePrompt, content, code);
    }

    private String buildFixPrompt(String code, String buildOutput) {
        return Format.format(fixPrompt, code, buildOutput);
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
        Utils.removeDirectory("/tmp/kiwi-works/1000015268");
        var apikeyPath = "/Users/leen/develop/gemini/apikey";
        var apikey = Files.readString(Path.of(apikeyPath));
        var chatService = new AigcService(
                new GeminiModel("gemini-2.5-pro", apikey),
                new DefaultKiwiCompiler(
                        Path.of("/tmp/kiwi-works"),
                        new DeployClient("http://localhost:8080", HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .build()))
        );
        chatService.generate(new GenerateRequest(
                1000015268L,
                "Create a todo list application"
        ));
    }

}
