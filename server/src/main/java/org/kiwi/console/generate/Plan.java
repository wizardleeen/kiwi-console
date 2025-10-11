package org.kiwi.console.generate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.kiwi.console.kiwi.Tech;

import javax.annotation.Nullable;
import java.util.List;

public record Plan(
        @Nullable String appName,
        List<Task> tasks
) {


    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = CreateModuleTask.class, name = "CREATE_MODULE"),
            @JsonSubTypes.Type(value = ModifyModuleTask.class, name = "MODIFY_MODULE"),
            @JsonSubTypes.Type(value = DeleteModuleTask.class, name = "DELETE_MODULE"),
            @JsonSubTypes.Type(value = TestTask.class, name = "TEST_MODULE"),
            @JsonSubTypes.Type(value = DataTask.class, name = "DATA"),
    })
    public sealed interface Task {

        String getType();

        String moduleName();

    }

    public sealed interface GenerateTask extends Task {

        String suggestion();

    }

    public record CreateModuleTask(
            String moduleName,
            String description,
            List<String> dependencyNames,
            Tech tech,
            String suggestion
    ) implements GenerateTask {

        @Override
        public String getType() {
            return "CREATE_MODULE";
        }
    }

    public record ModifyModuleTask(
            String moduleName,
            String suggestion
    ) implements GenerateTask {

        @Override
        public String getType() {
            return "MODIFY_MODULE";
        }
    }

    public record DeleteModuleTask(
            String moduleName,
            String suggestion
    ) implements Task {

        @Override
        public String getType() {
            return "DELETE_MODULE";
        }
    }

    public record TestTask(
            String moduleName
    ) implements Task {

        @Override
        public String getType() {
            return "TEST_MODULE";
        }
    }

    public record DataTask(
            String moduleName,
            String suggestion
    ) implements Task {

        @Override
        public String getType() {
            return "DATA";
        }
    }


}
