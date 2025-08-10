package org.kiwi.console.generate;

import java.io.InputStream;

public class MockDeployService implements DeployService {
    @Override
    public String deploy(long appId, InputStream input) {
        return null;
    }

    @Override
    public String getDeployStatus(long appId, String deployId) {
        return null;
    }

    @Override
    public void revert(long appId) {

    }
}
