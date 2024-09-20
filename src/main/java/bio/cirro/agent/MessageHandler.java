package bio.cirro.agent;

import bio.cirro.agent.dto.PortalMessage;
import bio.cirro.agent.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.dto.RunAnalysisResponseMessage;
import bio.cirro.agent.dto.UnknownMessage;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Singleton
@Slf4j
public class MessageHandler {
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
        log.info("{}", runAnalysisCommandMessage);
        return RunAnalysisResponseMessage.builder()
                .output("Analysis complete")
                .exitStatus(0)
                .datasetId(runAnalysisCommandMessage.getDatasetId())
                .build();
    }
}
