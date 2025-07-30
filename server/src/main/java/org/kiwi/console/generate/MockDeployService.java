package org.kiwi.console.generate;

import java.io.InputStream;

public class MockDeployService implements DeployService {
    @Override
    public void deploy(long appId, InputStream input) {
    }

    @Override
    public void revert(long appId) {

    }
}
