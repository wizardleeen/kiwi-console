package org.kiwi.console.generate;

import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.browser.Page;
import org.kiwi.console.file.File;
import org.kiwi.console.generate.event.GenerationListener;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
class GenerationTask implements Task {
    private final List<GenerationListener> listeners = new CopyOnWriteArrayList<>();
    Exchange exchange;
    final App app;
    boolean cancelled;
    final Model model;
    final GenerationConfig genConfig;
    final boolean showAttempts;
    final List<File> attachments;
    private final ExchangeClient exchClient;
    private final User user;
    private final List<PlaywrightActions.GeneratedAction> actions = new ArrayList<>();
    private Page page;

    public GenerationTask(ExchangeClient exchClient, Exchange exchange, App app, User user, boolean showAttempts, GenerationConfig genConfig, List<File> attachments, @Nonnull GenerationListener listener, Model model) {
        this.exchClient = exchClient;
        this.genConfig = genConfig;
        this.attachments = attachments;
        this.model = model;
        listeners.add(listener);
        this.app = app;
        this.user = user;
        this.exchange = exchange;
        this.showAttempts = showAttempts;
    }

    int enterStageAndAttempt(StageType type) {
        if (exchange.getStatus() == ExchangeStatus.PLANNING)
            exchange.setStatus(ExchangeStatus.GENERATING);
        var stage = Stage.create(type);
        stage.addAttempt(Attempt.create());
        var stageIdx = exchange.getStages().size();
        exchange.addStage(stage);
        saveExchange();
        return stageIdx;
    }

    void startAttempt() {
        currentStage().addAttempt(Attempt.create());
        saveExchange();
    }

    void finishAttempt(boolean successful, @Nullable String errorMsg) {
        var attempt = currentAttempt();
        if (successful) {
            attempt.setStatus(AttemptStatus.SUCCESSFUL);
            currentStage().setStatus(StageStatus.SUCCESSFUL);
        } else {
            attempt.setStatus(AttemptStatus.FAILED);
            attempt.setErrorMessage(errorMsg);
        }
        saveExchange();
    }

    void finishGeneration(String productUrl, String sourceCodeUrl) {
        exchange.setProductURL(productUrl);
        exchange.setSourceCodeURL(sourceCodeUrl);
        saveExchange();
    }

    void finishTest(int result) {
        var attempt = currentAttempt();
        if (result == 0) {
            attempt.setStatus(AttemptStatus.SUCCESSFUL);
            currentStage().setStatus(StageStatus.SUCCESSFUL);
        } else if (result == 1) {
            attempt.setStatus(AttemptStatus.REJECTED);
            currentStage().setStatus(StageStatus.REJECTED);
        } else {
            attempt.setStatus(AttemptStatus.FAILED);
            currentStage().setStatus(StageStatus.FAILED);
        }
        exchange.setStatus(ExchangeStatus.SUCCESSFUL);
        saveExchange();
    }

    void abort(String errorMsg) {
        if (!cancelled) {
            exchange.fail(errorMsg);
            saveExchange();
        }
    }

    void destroy() {
        if (page != null)
            page.close();
    }

    private Attempt currentAttempt() {
        return exchange.getStages().getLast().getAttempts().getLast();
    }

    private Stage currentStage() {
        return exchange.getStages().getLast();
    }

    void failStage(int stageIdx, String errMsg) {
        exchange.getStages().get(stageIdx).fail(errMsg);
        saveExchange();
    }

    void saveExchange() {
        ensureNotCancelled();
        exchange.setLastHeartBeatAt(System.currentTimeMillis());
        exchange = exchClient.get(exchClient.save(exchange));
        sendProgress();
    }

    @Override
    public List<File> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<File> attachments) {
        this.attachments.clear();
        this.attachments.addAll(attachments);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    void reloadExchange() {
        exchange = exchClient.get(exchange.getId());
    }

    void sendProgress() {
        var it = listeners.iterator();
        while (it.hasNext()) {
            var listener = it.next();
            try {
                listener.onProgress(exchange.toDTO(Utils.safeCall(page, Page::getTargetId)));
            } catch (Exception e) {
                log.error("Failed to send progress", e);
                it.remove();
            }
        }
    }

    boolean isBackendSuccessful() {
        return isStageSuccessful(StageType.BACKEND);
    }

    boolean isFrontendSuccessful() {
        return isStageSuccessful(StageType.FRONTEND);
    }

    boolean isStageSuccessful(StageType type) {
        for (Stage stage : exchange.getStages()) {
            if (stage.getType() == type && !stage.getAttempts().isEmpty()
                    && stage.getAttempts().getLast().getStatus() == AttemptStatus.SUCCESSFUL)
                return true;
        }
        return false;
    }

    @Override
    public void onThought(String thoughtChunk) {
        System.out.print(thoughtChunk);
        for (var listener : listeners) {
            listener.onThought(thoughtChunk);
        }
    }

    @Override
    public void onContent(String contentChunk) {
        System.out.print(contentChunk);
        for (var listener : listeners) {
            listener.onContent(contentChunk);
        }
    }

    public void addListener(GenerationListener listener) {
        listeners.add(listener);
    }

    private void ensureNotCancelled() {
        if (cancelled)
            throw new BusinessException(ErrorCode.TASK_CANCELLED);
    }

    public List<PlaywrightActions.GeneratedAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public void addAction(PlaywrightActions.GeneratedAction action) {
        actions.add(action);
    }

    public boolean isTesting() {
        return exchange.isRunning() && !exchange.getStages().isEmpty() &&
                exchange.getStages().getLast().getType() == StageType.TEST;
    }

    public void cancel() {
        cancelled = true;
    }

    public User getUser() {
        return user;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }
}
