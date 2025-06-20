package org.kiwi.console.kiwi;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.kiwi.console.util.TextWriter;

@Data
@AllArgsConstructor
public class Attempt {
    private String id;
    private AttemptStatus status;
    private String errorMessage;

    public static Attempt create() {
        return new Attempt(null, AttemptStatus.RUNNING, null);
    }

    public void write(TextWriter writer) {
        writer.writeln("Attempt " + status.name());
        if (errorMessage != null) {
            writer.indent();
            writer.writeln(errorMessage);
            writer.deIndent();;
        }
    }
}
