package org.kiwi.console.schema.dto;

import java.util.List;

public record ConstructorDTO(
        List<ParameterDTO> parameters
) {
}
