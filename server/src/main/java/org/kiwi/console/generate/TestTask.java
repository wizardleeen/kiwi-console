package org.kiwi.console.generate;

import lombok.SneakyThrows;
import org.kiwi.console.kiwi.Tech;

public interface TestTask {

    @SneakyThrows
    TestResult runTest();

    Tech getTech();

    void close();

}
