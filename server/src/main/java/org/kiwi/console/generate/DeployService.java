package org.kiwi.console.generate;

import lombok.SneakyThrows;

import java.io.InputStream;

public interface DeployService {
    String deploy(long appId, boolean noBackup, InputStream input);

    @SneakyThrows
    String getDeployStatus(long appId, String deployId);

    void revert(long appId);

}
