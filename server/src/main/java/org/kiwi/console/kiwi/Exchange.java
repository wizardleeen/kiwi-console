package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.kiwi.console.generate.rest.ExchangeDTO;
import org.kiwi.console.util.TextWriter;
import org.kiwi.console.util.Utils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Exchange {
    private String id;
    private String appId;
    private String userId;
    private String prompt;
    private List<String> attachmentUrls;
    private ExchangeStatus status;
    private @Nullable String productURL;
    private @Nullable String managementURL;
    private @Nullable String sourceCodeURL;
    private @Nullable String errorMessage;
    private boolean first;
    private boolean skipPageGeneration;
    private long lastHeartBeatAt;
    private @Nullable String parentExchangeId;
    private int chainDepth;
    private boolean testOnly;
    private List<ExchangeTask> tasks;

    public static Exchange create(String appId,
                                  String userId,
                                  String prompt,
                                  List<String> attachmentUrls,
                                  boolean first,
                                  boolean skipPageGeneration,
                                  @Nullable String parentExchangeId,
                                  int chainDepth,
                                  boolean testOnly) {
        return new Exchange(null,
                appId,
                userId,
                prompt,
                attachmentUrls,
                ExchangeStatus.PLANNING,
                null,
                null,
                null,
                null,
                first,
                skipPageGeneration,
                0,
                parentExchangeId,
                chainDepth,
                testOnly,
                new ArrayList<>()
        );
    }

    public void addTask(ExchangeTask exchangeTask) {
        tasks.add(exchangeTask);
    }

    public boolean isRunning() {
        return status == ExchangeStatus.GENERATING || status == ExchangeStatus.PLANNING;
    }

    public String toString() {
        var writer = new TextWriter();
        write(writer);
        return writer.toString();
    }

    public void write(TextWriter writer) {
        writer.writeln("Status: " + status.name());
        if (productURL != null)
            writer.writeln("Product URL: " + productURL);
        writer.indent();
        for (var task : tasks) {
            task.write(writer);
        }
        writer.deIndent();
    }

    public void fail(String errorMessage) {
        if (!isRunning())
            throw new IllegalStateException("Exchange is not running");
        this.status = ExchangeStatus.FAILED;
        this.errorMessage = errorMessage;
        for (var task : tasks) {
            if(task.isRunning()) {
                task.setStatus(ExchangeTaskStatus.FAILED);
                for (Attempt attempt : task.getAttempts()) {
                    if (attempt.isRunning())
                        attempt.setStatus(AttemptStatus.FAILED);
                }
            }
        }
    }

    public ExchangeDTO toDTO() {
        return toDTO(null);
    }

    public ExchangeDTO toDTO(@Nullable String testPageId) {
        return new ExchangeDTO(
                id,
                appId,
                prompt,
                status.name(),
                productURL,
                managementURL,
                sourceCodeURL,
                errorMessage,
                attachmentUrls,
                Utils.map(tasks, ExchangeTask::toDTO),
                testPageId,
                chainDepth
        );
    }

    public boolean isModuleSuccessful(String moduleId) {
        var found = Utils.find(tasks, t -> t.getModuleId().equals(moduleId));
        return found != null && found.getStatus() == ExchangeTaskStatus.SUCCESSFUL;
    }

    public boolean hasSuccessfulTasks() {
        return tasks.stream().anyMatch(s -> s.getStatus() == ExchangeTaskStatus.SUCCESSFUL);
    }

    public ExchangeTask getTaskByModuleId(String moduleId) {
        return Utils.findRequired(tasks, t -> t.getModuleId().equals(moduleId));
    }

}
