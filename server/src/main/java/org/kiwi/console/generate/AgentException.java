package org.kiwi.console.generate;

public class AgentException extends RuntimeException {

    public AgentException(String message) {
        super(message);
    }

    public AgentException(Throwable cause) {
        super(cause);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
