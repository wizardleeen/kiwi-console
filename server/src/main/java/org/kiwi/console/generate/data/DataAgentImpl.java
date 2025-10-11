package org.kiwi.console.generate.data;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.generate.*;
import org.kiwi.console.patch.PatchReader;
import org.kiwi.console.util.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class DataAgentImpl implements DataAgent {

    private static final int MAX_RETRIES = 5;
    private final String kiwiServer;

    public DataAgentImpl(String kiwiServer) {
        this.kiwiServer = kiwiServer;
    }

    @Override
    @SneakyThrows
    public void run(DataManipulationRequest request) {
        var chat = request.model().createChat(false);
        var prompt = Format.format(request.promptTemplate(), kiwiServer, Long.toString(request.appId()), request.requirement(),
                request.code());
        var listener = request.listener();
        log.info("Data Manipulation Prompt\n{}", prompt);
        listener.onAttemptStart();
        var script = CodeSanitizer.sanitizeCode(Models.generateContent(chat, prompt, List.of(), request.abortController()));
        var r = executeScript(script);
        log.info("Exit code: {}, output\n{}", r.exitCode(), r.output());
        if (r.exitCode() == 0) {
            listener.onAttemptSuccess();
            return;
        }
        listener.onAttemptFailure(r.output());
        String errorMsg = r.output();
        for (var i = 0; i < MAX_RETRIES; i++) {
            listener.onAttemptStart();
            script = fix(chat, request.fixPromptTemplate(), errorMsg, request.abortController());
            var r1 = executeScript(script);
            log.info("Exit code: {}, output\n{}", r1.exitCode(), r1.output());
            if (r1.exitCode() == 0) {
                listener.onAttemptSuccess();
                return;
            }
            listener.onAttemptFailure(r1.output());
            errorMsg = r1.output();
        }
        throw new AgentException("Failed to manipulate data after %d retries".formatted(MAX_RETRIES));
    }

    private String fix(Chat chat, String fixPromptTempl, String errorMessage, AbortController abortController) {
        var prompt = Format.format(fixPromptTempl, errorMessage);
        return CodeSanitizer.sanitizeCode(Models.generateContent(chat, prompt, List.of(), abortController));
    }

    public static Utils.CommandResult executeScript(String javascriptCode) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("node");

        // *** THIS IS THE KEY LINE ***
        // Redirect the process's error stream to its standard output stream.
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // Write the JavaScript code to the process's standard input.
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
            writer.write(javascriptCode);
        }

        // Read the combined output from the single input stream.
        StringBuilder combinedOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                combinedOutput.append(line).append(System.lineSeparator());
            }
        }

        // Wait for the process to complete and check its exit code.
        int exitCode = process.waitFor();
//        if (exitCode != 0) {
//            // The process failed. The combinedOutput will contain the error message.
//            throw new IOException(
//                    "Node.js process exited with code " + exitCode + ". Combined output:\n" + combinedOutput
//            );
//        }

        return new Utils.CommandResult(exitCode, combinedOutput.toString());
    }

    @SneakyThrows
    public static void main(String[] args) {
        var appId = 1000252548;
        var host = "http://localhost:8080";
        var kiwiCompiler = new DefaultKiwiCompiler(
                Path.of("/Users/leen/develop/kiwi-works"),
                new MockDeployService()
        );
        var code = PatchReader.buildCode(kiwiCompiler.getSourceFiles(appId + ""));
        var promptTempl = Utils.loadResource("/prompt/data.md");
        var fixPromptTempl = Utils.loadResource("/prompt/data-fix.md");
        var requirement = "Initialize the system with some data";

        var agent = new DataAgentImpl(host);
        agent.run(
                new DataManipulationRequest(
                        promptTempl,
                        fixPromptTempl,
                        appId,
                        requirement,
                        code,
                        new CodeAgentListener() {
                            @Override
                            public void onAttemptStart() {

                            }

                            @Override
                            public void onAttemptSuccess() {

                            }

                            @Override
                            public void onAttemptFailure(String error) {

                            }
                        },
                new GeminiModel("gemini-2.5-pro"),
                () -> false
                )
        );
    }

}
