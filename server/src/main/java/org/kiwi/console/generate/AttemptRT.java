package org.kiwi.console.generate;

import org.kiwi.console.kiwi.Attempt;
import org.kiwi.console.kiwi.AttemptStatus;

import javax.annotation.Nullable;

public class AttemptRT {

    private final String id;
    private AttemptStatus status;
    private @Nullable String errorMessage;
    private final ExchangeTaskRT task;

    public AttemptRT(String id, ExchangeTaskRT task) {
        this.id = id;
        this.task = task;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
        task.getExchange().onChange();
    }

    public void setErrorMessage(@Nullable String errorMsg) {
        this.errorMessage = errorMsg;
        task.getExchange().onChange();
    }

    public Attempt build() {
        return new Attempt(id, status, errorMessage);
    }

    public String getId() {
        return id;
    }

    public void update(Attempt attempt) {
        status = attempt.getStatus();
        errorMessage = attempt.getErrorMessage();
    }

    public void fail(String error) {
        status = AttemptStatus.FAILED;
        errorMessage = error;
    }
}
