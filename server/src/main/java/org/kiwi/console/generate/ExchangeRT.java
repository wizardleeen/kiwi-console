package org.kiwi.console.generate;

import org.kiwi.console.generate.rest.ExchangeDTO;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.Utils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ExchangeRT {

    public static ExchangeRT from(Exchange exchange, AppRT app, ExchangeClient exchClient) {
        var exch = new ExchangeRT(exchange.getId(), app, exchClient);
        exch.update(exchange);
        return exch;
    }

    private final String id;
    private String appId;
    private String userId;
    private String requirement;
    private ExchangeStatus status;
    private List<String> attachmentUrls;
    private String productURL;
    private String managementURL;
    private String sourceCodeURL;
    private String errorMessage;
    private boolean first;
    private long lastHeartBeatAt;
    private @Nullable String pageId;
    private final ExchangeClient exchClient;
    private Consumer<ExchangeDTO> listener;
    private final List<ExchangeTaskRT> tasks = new ArrayList<>();
    private final AppRT app;

    public ExchangeRT(String id, AppRT app, ExchangeClient exchClient) {
        this.id = id;
        this.exchClient = exchClient;
        this.app = app;
    }

    String getAppId() {
        return appId;
    }

    void onChange() {
        onChange(true);
    }

    void onChange(boolean ensureNotCancelled) {
        if (ensureNotCancelled)
            ensureNotCancelled();
        lastHeartBeatAt = System.currentTimeMillis();
        var exch = saveExchange(build());
        listener.accept(exch.toDTO(pageId));
    }

    private Exchange saveExchange(Exchange exch) {
        return exchClient.get(exchClient.save(exch));
    }

    private Exchange build() {
        return new Exchange(
                id,
                appId,
                userId,
                requirement,
                attachmentUrls,
                status,
                productURL,
                managementURL,
                sourceCodeURL,
                errorMessage,
                first,
                false,
                lastHeartBeatAt,
                null,
                0,
                false,
                Utils.map(tasks, ExchangeTaskRT::build)
        );
    }

    public ExchangeStatus getStatus() {
        return status;
    }

    public void setStatus(ExchangeStatus status) {
        this.status = status;
        onChange();
    }

    public List<ExchangeTaskRT> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public @Nullable ExchangeTaskRT findLastTaskByModule(String moduleId) {
        ExchangeTaskRT found = null;
        for (ExchangeTaskRT task : tasks) {
            if (moduleId.equals(task.getModuleId()))
                found = task;
        }
        return found;
    }

    public ExchangeTaskRT startTask(ModuleRT module) {
        var exch = build();
        exch.addTask(new ExchangeTask(module.id(), module.name()));
        exch = saveExchange(exch);
        update(exch);
        onChange();
        return getTasks().getLast();
    }

    public AttemptRT startAttempt(ExchangeTaskRT task) {
        var exch = build();
        var t = Utils.findRequired(exch.getTasks(), s -> s.getId().equals(task.getId()));
        t.addAttempt(Attempt.create());
        exch = saveExchange(exch);
        update(exch);
        onChange();
        return task.getAttempts().getLast();
    }

    public String getRequirement() {
        return requirement;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isTesting() {
        for (ExchangeTaskRT task : tasks) {
            var mod = app.getModule(task.getModuleId());
            if (mod.type().isTestable() &&
                    (task.getStatus() == ExchangeTaskStatus.TESTING || task.getStatus() == ExchangeTaskStatus.SUCCESSFUL)) {
                return true;
            }
        }
        return false;
    }

    public String getProductURL() {
        return productURL;
    }

    public void setManagementURL(String managementURL) {
        this.managementURL = managementURL;
    }

    public void setProductURL(String productUrl) {
        this.productURL = productUrl;
        onChange();
    }

    public void setSourceCodeURL(String sourceCodeUrl) {
        this.sourceCodeURL = sourceCodeUrl;
        onChange();
    }

    public void fail(String errorMsg) {
        this.status = ExchangeStatus.FAILED;
        this.errorMessage = errorMsg;
        onChange();
    }

    public boolean isRunning() {
        return status == ExchangeStatus.PLANNING || status == ExchangeStatus.GENERATING;
    }

    public String getId() {
        return id;
    }

    public void reload() {
        update(exchClient.get(id));
        onChange(false);
    }

    private void ensureNotCancelled() {
        if (status == ExchangeStatus.CANCELLED)
            throw new BusinessException(ErrorCode.TASK_CANCELLED);
    }

    public void setPageId(@Nullable String pageId) {
        this.pageId = pageId;
    }

    private void update(Exchange exchange) {
        appId = exchange.getAppId();
        userId = exchange.getUserId();
        requirement = exchange.getPrompt();
        status = exchange.getStatus();
        attachmentUrls = exchange.getAttachmentUrls();
        productURL = exchange.getProductURL();
        managementURL = exchange.getManagementURL();
        sourceCodeURL = exchange.getSourceCodeURL();
        errorMessage = exchange.getErrorMessage();
        first = exchange.isFirst();
        lastHeartBeatAt = exchange.getLastHeartBeatAt();
        var taskMap = Utils.toMap(tasks, ExchangeTaskRT::getId);
        tasks.clear();
        for (ExchangeTask exchangeTask : exchange.getTasks()) {
            var t =  taskMap.get(Objects.requireNonNull(exchangeTask.getId()));
            if (t == null) {
                t = new ExchangeTaskRT(exchangeTask.getId(), this);
            }
            tasks.add(t);
            t.update(exchangeTask);
        }
    }

    public void sendProgress() {
        listener.accept(build().toDTO(pageId));
    }

    public boolean isCancelled() {
        return status == ExchangeStatus.CANCELLED;
    }

    public void setListener(Consumer<ExchangeDTO> listener) {
        this.listener = listener;
    }

    public void cancel() {
        ensureNotCancelled();
        status = ExchangeStatus.CANCELLED;
        onChange(false);
    }

    public void abort(String errorMessage) {
        status = ExchangeStatus.FAILED;
        this.errorMessage = errorMessage;
        onChange();
    }
}
