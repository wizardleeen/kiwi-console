package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Application {
    private String id;
    private String name;
    private String ownerId;
    private long systemAppId;
    private List<String> membersIds;

    public static Application create(String name, String ownerId) {
        return new Application(null, name, ownerId, -1, List.of());
    }

}
