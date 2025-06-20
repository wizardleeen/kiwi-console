package org.kiwi.console.generate;

import org.kiwi.console.kiwi.SystemApp;
import org.kiwi.console.kiwi.SystemAppClient;
import org.kiwi.console.util.Result;

public class MockSysAppClient implements SystemAppClient {
    @Override
    public Result<Long> save(SystemApp application) {
        return Result.success(TestConstants.SYS_APP_ID);
    }

    @Override
    public Result<Void> delete(long id) {
        return null;
    }
}
