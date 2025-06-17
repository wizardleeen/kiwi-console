package org.kiwi.console.genai;

import java.io.InputStream;

public interface DeployService {
    String deploy(long appId, String token, InputStream input);
}
