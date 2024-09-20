package bio.cirro.agent.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@Serdeable
public class RunAnalysisCommandMessage extends PortalMessage {
    String datasetId;
}
