package org.kiwi.console.schema.dto;

public record ArrayTypeDTO(TypeDTO elementType) implements TypeDTO {
    @Override
    public String getKind() {
        return "array";
    }
}
