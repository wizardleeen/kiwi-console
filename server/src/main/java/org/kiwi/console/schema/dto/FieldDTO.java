package org.kiwi.console.schema.dto;

public record FieldDTO(
    String access,
    String name,
    TypeDTO type,
    boolean summary,
    String label
) {
}
