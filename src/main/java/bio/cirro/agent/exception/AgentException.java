package bio.cirro.agent.exception;

/**
 * Exceptions should be thrown when the agent encounters an error that it cannot recover from.
 */
public class AgentException extends RuntimeException {
    public AgentException(String message) {
        super(message);
    }
}
