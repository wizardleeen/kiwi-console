package org.kiwi.console.schema.dto;

public record PrimitiveTypeDTO(String name) implements TypeDTO {
    @Override
    public String getKind() {
        return "primitive";
    }
}
