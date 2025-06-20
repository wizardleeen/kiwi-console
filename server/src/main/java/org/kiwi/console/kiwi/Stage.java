package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.kiwi.console.util.TextWriter;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Stage {
    private String id;
    private StageType type;
    private StageStatus status;
    private List<Attempt> attempts;

    public static Stage create(StageType type) {
        return new Stage(null, type, StageStatus.GENERATING, new ArrayList<>());
    }

    public void addAttempt(Attempt attempt) {
        attempts.add(attempt);
    }

    public void write(TextWriter writer) {
        writer.writeln("Stage " + type.name() + ": " + status.name());
        writer.indent();
        attempts.forEach(attempt -> attempt.write(writer));
        writer.deIndent();
    }
}
