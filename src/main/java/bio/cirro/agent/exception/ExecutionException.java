package bio.cirro.agent.exception;

import lombok.Getter;

/**
 * Exceptions when the agent encounters an error during job execution.
 */
@Getter
public class ExecutionException extends RuntimeException {
    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
