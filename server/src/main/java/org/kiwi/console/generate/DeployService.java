package org.kiwi.console.generate;

import org.kiwi.console.util.Result;

import java.io.InputStream;

public interface DeployService {
    Result<String> deploy(long appId, String token, InputStream input);
}
