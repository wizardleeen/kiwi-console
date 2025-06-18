package org.kiwi.console.genai;

public class PromptParser {
    public static final String createDescStart = "Here is the description for the program to be generated:\n";
    public static final String updateDescStart = "Here is the description of how the user want to modify the program:\n";
    public static final String codeStart = "Here is the existing code:\n";
    public static final String errorStart = "Here is the build error:\n";

    public static Prompt parse(String text) {
        if (text.startsWith("Your task is to modify")) {
            var idx1 = text.lastIndexOf(updateDescStart);
            var idx2 = text.lastIndexOf(codeStart);
            if (idx1 == -1 || idx2 == -1)
                throw new IllegalArgumentException("Invalid prompt");
            var prompt = text.substring(idx1 + updateDescStart.length(), idx2 - 1);
            var code = text.substring(idx2 + codeStart.length());
            return new Prompt(PromptKind.UPDATE, prompt, code);
        } else if (text.startsWith("Your task is to generate")) {
            var idx = text.lastIndexOf(createDescStart);
            if (idx == -1)
                throw new IllegalArgumentException("Invalid prompt");
            var prompt = text.substring(idx + createDescStart.length());
            return new Prompt(PromptKind.CREATE, prompt, null);
        } else if (text.startsWith("The generated code didn't compile.")) {
            var idx = text.lastIndexOf(errorStart);
            if (idx == -1)
                throw new IllegalArgumentException("Invalid prompt");
            var prompt = text.substring(idx + errorStart.length());
            return new Prompt(PromptKind.FIX, prompt, null);
        } else
            throw new IllegalArgumentException("Invalid prompt");
    }

}
