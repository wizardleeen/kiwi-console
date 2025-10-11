package org.kiwi.console.generate.rest;

public record ExchangeTaskDTO(
        String id,
        String moduleId,
        String moduleName,
        String type,
        String status
) {
}
