package org.kiwi.console.generate;

import org.kiwi.console.kiwi.Tech;

public record ModuleInfo(
        String name,
        Tech tech,
        String description,
        String code
) {
}
