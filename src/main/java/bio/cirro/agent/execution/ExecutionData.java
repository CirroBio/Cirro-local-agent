package bio.cirro.agent.execution;

import bio.cirro.agent.models.Status;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@MappedEntity("executions")
@Introspected
@Data
@Builder
public class ExecutionData {
    @Id
    private String id;
    @Column(length = 10000)
    private String messageDataJson;
    private Status status;
    @Column(length = 10000)
    @Nullable
    private String startOutputJson;
    @Column(length = 10000)
    @Nullable
    private String finishOutputJson;
    @Nullable
    private Instant createdAt;
    @Nullable
    private Instant finishedAt;
}
