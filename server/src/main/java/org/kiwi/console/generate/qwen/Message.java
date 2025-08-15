package org.kiwi.console.generate.qwen;

import java.util.List;

public record Message(String role, List<Content> content) {
}
