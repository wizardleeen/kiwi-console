package org.kiwi.console.generate.qwen;

import java.util.List;

public record Response(String id, List<Choice> choices) {
}
