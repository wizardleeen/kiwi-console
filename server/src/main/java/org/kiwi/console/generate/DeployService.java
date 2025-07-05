package org.kiwi.console.generate;

import java.io.InputStream;

public interface DeployService {
    void deploy(long appId, InputStream input);
}
