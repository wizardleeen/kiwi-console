package org.kiwi.console.generate;

import org.kiwi.console.kiwi.Attempt;
import org.kiwi.console.kiwi.ExchangeTask;
import org.kiwi.console.kiwi.ExchangeTaskStatus;
import org.kiwi.console.util.Utils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExchangeTaskRT {
    private final String id;
    private String moduleId;
    private String moduleName;
    private ExchangeTaskStatus status;
    private @Nullable String errorMessage;
    private final ExchangeRT exchange;
    private final List<AttemptRT> attempts = new ArrayList<>();

    public ExchangeTaskRT(String id, ExchangeRT exchange) {
        this.id = id;
        this.exchange = exchange;
    }

    public List<AttemptRT> getAttempts() {
        return Collections.unmodifiableList(attempts);
    }

    public AttemptRT getLastAttempt() {
        if (attempts.isEmpty())
            throw new NoSuchElementException("Task " + id + " has no attempts");
        return attempts.getLast();
    }

    public void fail(String errMsg) {
        status = ExchangeTaskStatus.FAILED;
        errorMessage = errMsg;
    }

    public void addAttempt() {
        exchange.startAttempt(this);
    }

    public void setStatus(ExchangeTaskStatus status) {
        this.status = status;
        exchange.onChange();
    }

    public ExchangeTaskStatus getStatus() {
        return status;
    }

    ExchangeTask build() {
        return new ExchangeTask(
                id,
                moduleId,
                moduleName,
                status,
                errorMessage,
                Utils.map(attempts, AttemptRT::build)
        );
    }

    public ExchangeRT getExchange() {
        return exchange;
    }

    public String getId() {
        return id;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void update(ExchangeTask exchangeTask) {
        moduleId = exchangeTask.getModuleId();
        moduleName = exchangeTask.getModuleName();
        status = exchangeTask.getStatus();
        errorMessage = exchangeTask.getErrorMessage();
        var attemptMap = attempts.stream().collect(Collectors.toUnmodifiableMap(AttemptRT::getId, Function.identity()));
        attempts.clear();
        for (Attempt attempt : exchangeTask.getAttempts()) {
            var a = attemptMap.get(attempt.getId());
            if (a == null)
                a = new AttemptRT(attempt.getId(), this);
            attempts.add(a);
            a.update(attempt);
        }
    }
}
