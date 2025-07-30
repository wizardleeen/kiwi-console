package org.kiwi.console.generate;

import org.kiwi.console.file.File;

import java.util.List;

public interface Chat {

    void send(String text, List<File> attachments, ChatStreamListener listener, ChatController ctrl);

}
