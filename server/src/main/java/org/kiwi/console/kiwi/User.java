package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class User {
    private String id;
    private String name;
    private String sysUserId;
    private List<App> apps;
    private String genConfigId;
}
