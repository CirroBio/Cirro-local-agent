package bio.cirro.agent;

import bio.cirro.agent.dto.PortalMessage;
import bio.cirro.agent.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.dto.RunAnalysisResponseMessage;
import bio.cirro.agent.dto.UnknownMessage;
import bio.cirro.agent.exception.ExecutionException;
import bio.cirro.agent.execution.ExecutionCreateService;
import bio.cirro.agent.models.Status;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Handles messages received by the agent
 * and returns a response message if applicable.
 */
@Singleton
@Slf4j
@AllArgsConstructor
public class MessageHandler {
    private final ExecutionCreateService executionCreateService;

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
            var execution = executionCreateService.createSession(runAnalysisCommandMessage);
            return RunAnalysisResponseMessage.builder()
                    .output(execution.getOutput().stdout())
                    .nativeJobId(execution.getOutput().localJobId())
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
