package bio.cirro.agent.messaging.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@Serdeable
@Builder
public class StopAnalysisMessage extends PortalMessage {
    String datasetId;
    String projectId;
    String reason;
}
