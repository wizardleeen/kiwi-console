package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.kiwi.console.generate.rest.ExchangeTaskDTO;
import org.kiwi.console.util.TextWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
public class ExchangeTask {

    private String id;
    private final String moduleId;
    private String moduleName;
    private final ExchangeTaskType type;
    private ExchangeTaskStatus status = ExchangeTaskStatus.GENERATING;
    private String errorMessage;
    private List<Attempt> attempts;

    public ExchangeTask(String moduleId, String moduleName, ExchangeTaskType type) {
        this.moduleId = moduleId;
        this.moduleName = moduleName;
        this.type = type;
        attempts = new ArrayList<>();
    }

    public String getModuleId() {
        return moduleId;
    }

    public ExchangeTaskStatus getStatus() {
        return status;
    }

    public List<Attempt> getAttempts() {
        return Collections.unmodifiableList(attempts);
    }

    public void addAttempt(Attempt attempt) {
        attempts.add(attempt);
    }

    public void setStatus(ExchangeTaskStatus status) {
        this.status = status;
    }

    public ExchangeTaskType getType() {
        return type;
    }

    public void write(TextWriter writer) {
        writer.writeln("Task " + moduleName + ": " + status.name());
        writer.indent();
        attempts.forEach(attempt -> attempt.write(writer));
        writer.deIndent();
    }

    public boolean isRunning() {
        return status == ExchangeTaskStatus.GENERATING;
    }

    public void fail(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ExchangeTaskDTO toDTO() {
        return new ExchangeTaskDTO(id, moduleId, moduleName, type.name(), status.name());
    }
}
