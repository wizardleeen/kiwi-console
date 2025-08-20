package org.kiwi.console.generate;

import org.kiwi.console.file.File;

import java.util.List;

public interface Task extends ChatStreamListener {

    List<File> getAttachments();

    boolean isCancelled();

}
