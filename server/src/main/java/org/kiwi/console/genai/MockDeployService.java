package org.kiwi.console.genai;

import org.kiwi.console.util.Result;

import java.io.InputStream;

public class MockDeployService implements DeployService {
    @Override
    public Result<String> deploy(long appId, String token, InputStream input) {
        return Result.success("");
    }
}
