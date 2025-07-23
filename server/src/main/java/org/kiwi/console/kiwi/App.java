package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class App {
    private String id;
    private String name;
    private String ownerId;
    private long systemAppId;
    private List<String> memberIds;
    private String genConfigId;

    public static App create(String name, String ownerId) {
        return new App(null, name, ownerId, -1, List.of(), "");
    }

}
