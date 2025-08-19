package org.kiwi.console.schema.dto;

import javax.annotation.Nullable;

public record FieldDTO(
    String access,
    String name,
    TypeDTO type,
    boolean summary,
    String label,
    // Possible values: date
    @Nullable String numberFormat
) {
}
