package bio.cirro.agent.messaging.dto;

import bio.cirro.agent.models.Status;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Value
@Serdeable
@Builder
public class AnalysisUpdateMessage extends PortalMessage {
    String datasetId;
    String projectId;
    Status status;
    String message;
    Map<String, Object> details;
}
