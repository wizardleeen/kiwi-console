package org.kiwi.console.kiwi;

import lombok.Data;

import java.util.List;

@Data
public class User {
    private String name;
    private String sysUserId;
    private List<App> apps;
}
