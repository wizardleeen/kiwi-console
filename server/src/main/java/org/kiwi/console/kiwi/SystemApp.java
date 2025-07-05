package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SystemApp {
    private Long id;
    private String name;
    private String ownerId;
}
