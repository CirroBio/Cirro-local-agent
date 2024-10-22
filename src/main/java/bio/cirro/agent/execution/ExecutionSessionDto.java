package bio.cirro.agent.execution;

import bio.cirro.agent.models.Code;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

@Serdeable
public record ExecutionSessionDto(
        String sessionId,
        String workingDirectory,
        String status,
        String username,
        Code workflowCode,
        Instant createdAt
) {

    public static ExecutionSessionDto from(ExecutionSession session) {
        return new ExecutionSessionDto(
                session.getSessionId(),
                session.getWorkingDirectory().toString(),
                session.getStatus().name(),
                session.getUsername(),
                session.getMessageData().getWorkflowCode(),
                session.getCreatedAt()
        );
    }
}
