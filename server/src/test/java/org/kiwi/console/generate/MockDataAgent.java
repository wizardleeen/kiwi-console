package org.kiwi.console.generate;

import org.kiwi.console.generate.data.DataAgent;
import org.kiwi.console.generate.data.DataManipulationRequest;

public class MockDataAgent implements DataAgent {
    @Override
    public void run(DataManipulationRequest request) {
        request.listener().onAttemptStart();
        request.listener().onAttemptSuccess();
    }
}
