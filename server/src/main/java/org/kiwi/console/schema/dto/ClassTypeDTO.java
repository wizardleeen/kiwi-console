package org.kiwi.console.schema.dto;

public record ClassTypeDTO(String qualifiedName) implements TypeDTO {
    @Override
    public String getKind() {
        return "class";
    }
}
