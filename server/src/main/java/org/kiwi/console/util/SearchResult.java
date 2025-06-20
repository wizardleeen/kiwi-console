package org.kiwi.console.util;

import java.util.List;

public record SearchResult<T>(List<T> items, long total) {
}
