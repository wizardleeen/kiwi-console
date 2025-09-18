package org.kiwi.console.app.rest;

import java.util.List;

public record ModuleDTO(String id, String name, String type, String configId, String description, List<String> dependencyIds) {
}
