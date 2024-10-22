package bio.cirro.agent.execution;

import bio.cirro.agent.models.Code;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

@Serdeable
public record ExecutionDto(
        String datasetId,
        String workingDirectory,
        String status,
        String username,
        Code workflowCode,
        Instant createdAt
) {

    public static ExecutionDto from(Execution execution) {
        return new ExecutionDto(
                execution.getDatasetId(),
                execution.getWorkingDirectory().toString(),
                execution.getStatus().name(),
                execution.getUsername(),
                execution.getMessageData().getWorkflowCode(),
                execution.getCreatedAt()
        );
    }
}
