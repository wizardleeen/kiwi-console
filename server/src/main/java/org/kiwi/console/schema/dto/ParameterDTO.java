package org.kiwi.console.schema.dto;

public record ParameterDTO(
        String name,
        TypeDTO type,
        String label
) {
}
