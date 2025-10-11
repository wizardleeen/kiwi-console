package org.kiwi.console.generate.data;

import lombok.SneakyThrows;

public interface DataAgent {
    @SneakyThrows
    void run(DataManipulationRequest request);
}
