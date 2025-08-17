package org.kiwi.console.generate;

import org.kiwi.console.file.File;
import org.kiwi.console.generate.rest.AutoTestAction;
import org.kiwi.console.kiwi.App;
import org.kiwi.console.kiwi.Exchange;
import org.kiwi.console.kiwi.GenerationConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutoTestTask implements Task {
    private final App app;
    private final GenerationConfig genConfig;
    private final Exchange exch;
    private List<File> attachments = List.of();
    private final List<AutoTestAction> actions = new ArrayList<>();
    private final Model model;

    public AutoTestTask(App app, GenerationConfig genConfig, Exchange exch, Model model) {
        this.app = app;
        this.genConfig = genConfig;
        this.exch = exch;
        this.model = model;
    }

    @Override
    public void onThought(String thoughtChunk) {
        System.out.print(thoughtChunk);
    }

    @Override
    public void onContent(String contentChunk) {
        System.out.print(contentChunk);
    }

    @Override
    public List<File> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public void setAttachments(List<File> attachments) {
        this.attachments = attachments;
    }

    @Override
    public void sendHeartBeatIfRequired() {

    }

    public void addAction(AutoTestAction action) {
        actions.add(action);
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    public GenerationConfig getGenConfig() {
        return genConfig;
    }

    public Exchange getExch() {
        return exch;
    }

    public App getApp() {
        return app;
    }

    public Model getModel() {
        return model;
    }

    public List<AutoTestAction> getActions() {
        return actions;
    }
}
