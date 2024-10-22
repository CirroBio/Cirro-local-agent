package bio.cirro.agent.execution;

import bio.cirro.agent.models.Code;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Duration;
import java.time.Instant;

@Serdeable
public record ExecutionDto(
        String datasetId,
        String projectId,
        String workingDirectory,
        String status,
        String username,
        Code workflowCode,
        Instant createdAt
) {
    @JsonProperty
    public long getElapsedTimeSeconds() {
        return Duration.between(createdAt, Instant.now()).getSeconds();
    }

    public static ExecutionDto from(Execution execution) {
        return new ExecutionDto(
                execution.getDatasetId(),
                execution.getProjectId(),
                execution.getWorkingDirectory().toString(),
                execution.getStatus().name(),
                execution.getUsername(),
                execution.getMessageData().getWorkflowCode(),
                execution.getCreatedAt()
        );
    }
}
