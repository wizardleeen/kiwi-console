package org.kiwi.console.kiwi;

import java.util.List;

public record SystemUser(String name, String loginName, String password, List<String> roleIds) {
}
