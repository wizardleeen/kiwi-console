package org.kiwi.console.generate.k2;

import java.util.List;

public record Message(String role, List<Content> content) {
}
