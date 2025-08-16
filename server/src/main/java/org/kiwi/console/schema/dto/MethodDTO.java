package org.kiwi.console.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MethodDTO(
        String access,
        @JsonProperty("abstract")
        boolean isAbstract,
        String name,
        List<ParameterDTO> parameters,
        TypeDTO returnType,
        String label
) {
}
