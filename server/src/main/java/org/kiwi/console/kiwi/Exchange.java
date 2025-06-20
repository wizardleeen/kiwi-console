package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.kiwi.console.util.TextWriter;

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
    private ExchangeStatus status;
    private List<Stage> stages;
    private @Nullable String productURL;
    private @Nullable String managementURL;
    private @Nullable String errorMessage;
    private boolean first;
    private boolean skipPageGeneration;

    public static Exchange create(String appId, String userId, String prompt, boolean first, boolean skipPageGeneration) {
        return new Exchange(null,
                appId,
                userId,
                prompt,
                ExchangeStatus.PLANNING,
                new ArrayList<>(),
                null,
                null,
                null,
                first,
                skipPageGeneration);
    }

    public void addStage(Stage stage) {
        stages.add(stage);
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

}
