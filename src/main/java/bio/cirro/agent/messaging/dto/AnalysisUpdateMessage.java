package bio.cirro.agent.messaging.dto;

import bio.cirro.agent.models.Status;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Map;

/**
 * Message sent by the agent to the portal to update the status of an analysis.
 */
@EqualsAndHashCode(callSuper = true)
@Value
@Serdeable
@Builder
public class AnalysisUpdateMessage extends PortalMessage {
    String datasetId;
    String projectId;
    String nativeJobId;
    Status status;
    String message;
    Map<String, Object> details;
}
