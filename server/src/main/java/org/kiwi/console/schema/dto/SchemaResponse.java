package org.kiwi.console.schema.dto;

import java.util.List;

public record SchemaResponse(
        List<ClassDTO> classes
) {
}
