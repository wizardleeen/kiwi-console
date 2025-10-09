package org.kiwi.console.kiwi;

public record CreateAppRequest(String name, long kiwiAppId, String ownerId) {
}
