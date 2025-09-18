package org.kiwi.console.generate;

import org.kiwi.console.kiwi.Tech;

import javax.annotation.Nullable;
import java.util.List;

public record Plan(
        @Nullable String appName,
        List<ModulePlan> modulePlans
) {



}

record ModulePlan(
        String moduleName,
        Operation operation,
        String description,
        List<String> dependencyNames,
        Tech tech,
        String suggestion
) {

    public static ModulePlan modifyAndTest(String moduleName, String suggestion) {
        return new ModulePlan(moduleName, Operation.MODIFY_AND_TEST, null, null, null, suggestion);
    }

    boolean generationRequired() {
        return operation.generationRequired();
    }

    boolean testRequired() {
        return operation.testRequired();
    }

}


enum Operation {
    CREATE_AND_TEST {
        @Override
        public boolean testRequired() {
            return true;
        }

        @Override
        public boolean generationRequired() {
            return true;
        }
    },
    MODIFY_AND_TEST {
        @Override
        public boolean testRequired() {
            return true;
        }

        @Override
        public boolean generationRequired() {
            return true;
        }
    },
    TEST {
        @Override
        public boolean testRequired() {
            return true;
        }

        @Override
        public boolean generationRequired() {
            return false;
        }
    },

    ;

    public abstract boolean testRequired();

    public abstract boolean generationRequired();




}