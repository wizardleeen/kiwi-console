package org.kiwi.console.generate;

import org.kiwi.console.kiwi.SystemApp;
import org.kiwi.console.kiwi.KiwiAppClient;

public class MockKiwiAppClient implements KiwiAppClient {
    @Override
    public long save(SystemApp application) {
        return TestConstants.SYS_APP_ID;
    }

    @Override
    public void delete(long id) {
    }
}
