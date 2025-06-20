package org.kiwi.console.kiwi;

public record UpdateNameRequest(
        String applicationId,
        String name
) {
}
