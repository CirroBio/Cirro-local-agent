package bio.cirro.agent;

import bio.cirro.agent.dto.PortalMessage;
import bio.cirro.agent.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.dto.RunAnalysisResponseMessage;
import bio.cirro.agent.dto.UnknownMessage;
import bio.cirro.agent.exception.ExecutionException;
import bio.cirro.agent.execution.ExecutionService;
import bio.cirro.agent.models.Status;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Handles messages received by the agent
 * and returns a response message if applicable.
 */
@Singleton
@Slf4j
public class MessageHandler {
    private ExecutionService executionSessionService;

    public Optional<PortalMessage> handleMessage(PortalMessage message) {
        return switch (message) {
            case RunAnalysisCommandMessage runAnalysisCommandMessage ->
                    Optional.of(handleRunAnalysisCommand(runAnalysisCommandMessage));
            case UnknownMessage unknownMessage -> {
                log.warn("Received unknown message: {}", unknownMessage.getRawMessage());
                yield Optional.empty();
            }
            default -> {
                log.debug("Received message: {}", message);
                yield Optional.empty();
            }
        };
    }

    private RunAnalysisResponseMessage handleRunAnalysisCommand(RunAnalysisCommandMessage runAnalysisCommandMessage) {
        try {
            var execution = executionSessionService.createSession(runAnalysisCommandMessage);
            return RunAnalysisResponseMessage.builder()
                    .output(execution.getSessionId())
                    .status(Status.PENDING)
                    .datasetId(runAnalysisCommandMessage.getDatasetId())
                    .build();
        } catch (ExecutionException e) {
            var message = String.format("Error running analysis: %s", e.getMessage());
            log.error(message, e);
            return RunAnalysisResponseMessage.builder()
                    .output(e.getMessage())
                    .status(Status.FAILED)
                    .datasetId(runAnalysisCommandMessage.getDatasetId())
                    .build();
        }
    }
}
