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
    private List<Stage> stages;
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
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                first,
                skipPageGeneration,
                0,
                parentExchangeId,
                chainDepth,
                testOnly
        );
    }

    public void addStage(Stage stage) {
        stages.add(stage);
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
        for (Stage stage : stages) {
            stage.write(writer);
        }
        writer.deIndent();
    }

    public void fail(String errorMessage) {
        if (!isRunning())
            throw new IllegalStateException("Exchange is not running");
        this.status = ExchangeStatus.FAILED;
        this.errorMessage = errorMessage;
        for (Stage stage : stages) {
            if(stage.isRunning()) {
                stage.setStatus(StageStatus.FAILED);
                for (Attempt attempt : stage.getAttempts()) {
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
                Utils.map(stages, Stage::toDTO),
                testPageId,
                chainDepth
        );
    }

    public boolean isStageSuccessful(StageType stageType) {
        for (Stage stage : stages) {
            if (stage.getType() == stageType && stage.getStatus() == StageStatus.SUCCESSFUL)
                return true;
        }
        return false;
    }

    public boolean hasSuccessfulStages() {
        return stages.stream().anyMatch(s -> s.getStatus() == StageStatus.SUCCESSFUL);
    }

}
