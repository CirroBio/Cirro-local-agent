package bio.cirro.agent.messaging.dto;

import bio.cirro.agent.models.Status;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Message sent by the agent to the portal in response to a RunAnalysisCommandMessage.
 * It contains information of the submitted job.
 */
@Value
@EqualsAndHashCode(callSuper = true)
@Serdeable
@Builder
public class RunAnalysisResponseMessage extends PortalMessage {
    Status status;
    String nativeJobId;
    String output;
    String datasetId;
}