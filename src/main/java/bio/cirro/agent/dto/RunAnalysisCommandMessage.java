package bio.cirro.agent.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Message sent by the portal to the agent to request the agent to run an analysis.
 */
@EqualsAndHashCode(callSuper = true)
@Value
@Serdeable
public class RunAnalysisCommandMessage extends PortalMessage {
    String datasetId;
}
