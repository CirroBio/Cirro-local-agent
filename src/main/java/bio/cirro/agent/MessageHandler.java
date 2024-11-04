package bio.cirro.agent;

import bio.cirro.agent.exception.ExecutionException;
import bio.cirro.agent.execution.ExecutionCreateService;
import bio.cirro.agent.execution.ExecutionService;
import bio.cirro.agent.messaging.dto.AckMessage;
import bio.cirro.agent.messaging.dto.AnalysisUpdateMessage;
import bio.cirro.agent.messaging.dto.PortalMessage;
import bio.cirro.agent.messaging.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.messaging.dto.StopAnalysisMessage;
import bio.cirro.agent.messaging.dto.UnknownMessage;
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
    private final ExecutionService executionService;

    public Optional<PortalMessage> handleMessage(PortalMessage message) {
        return switch (message) {
            case RunAnalysisCommandMessage runAnalysisCommandMessage ->
                    Optional.of(handleRunAnalysisCommand(runAnalysisCommandMessage));
            case StopAnalysisMessage stopAnalysisMessage ->
                    Optional.of(handleStopAnalysisCommand(stopAnalysisMessage));
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

    private AnalysisUpdateMessage handleRunAnalysisCommand(RunAnalysisCommandMessage runAnalysisCommandMessage) {
        try {
            var execution = executionCreateService.create(runAnalysisCommandMessage);
            return AnalysisUpdateMessage.builder()
                    .datasetId(runAnalysisCommandMessage.getDatasetId())
                    .projectId(runAnalysisCommandMessage.getProjectId())
                    .nativeJobId(execution.getStartOutput().localJobId())
                    .message(execution.getStartOutput().stdout())
                    .status(Status.PENDING)
                    .build();
        } catch (ExecutionException e) {
            var message = String.format("Error running analysis: %s", e.getMessage());
            log.error(message, e);
            return AnalysisUpdateMessage.builder()
                    .datasetId(runAnalysisCommandMessage.getDatasetId())
                    .projectId(runAnalysisCommandMessage.getProjectId())
                    .nativeJobId(null)
                    .message(e.getMessage())
                    .status(Status.FAILED)
                    .build();
        }
    }

    private AckMessage handleStopAnalysisCommand(StopAnalysisMessage stopAnalysisMessage) {
        executionService.stopExecution(stopAnalysisMessage);
        return new AckMessage("Analysis stopped");
    }
}
