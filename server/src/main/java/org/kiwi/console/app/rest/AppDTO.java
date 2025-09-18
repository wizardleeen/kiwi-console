package org.kiwi.console.app.rest;

import java.util.List;

public record AppDTO(String id, String name, String ownerId, List<ModuleDTO> modules) {
}
