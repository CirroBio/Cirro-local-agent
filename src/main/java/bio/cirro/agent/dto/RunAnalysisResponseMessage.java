package bio.cirro.agent.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@Serdeable
@Builder
public class RunAnalysisResponseMessage extends PortalMessage {
    int exitStatus;
    String output;
    String datasetId;
}
