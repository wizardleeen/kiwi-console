package org.kiwi.console.generate.rest;

import org.kiwi.console.generate.AutoTestActionType;

public record AutoTestAction(
        AutoTestActionType type,
        String desc,
        String content
) {

    @Override
    public String toString() {
        return type.name() + "\n" + desc + "\n" + content + "\n@@@@";
    }


}
