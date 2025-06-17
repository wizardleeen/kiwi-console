package org.kiwi.console.genai;

import java.io.InputStream;

public class MockDeployService implements DeployService {
    @Override
    public String deploy(long appId, String token, InputStream input) {
        return "";
    }
}
