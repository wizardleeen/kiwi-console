package org.kiwi.console.object;

import java.util.Map;

public record SaveRequest(long appId, Map<String, Object> object) {
}
