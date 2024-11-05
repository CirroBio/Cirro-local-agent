package bio.cirro.agent.messaging.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Map;

/**
 * Message sent by the portal to the agent to request the agent to run an analysis.
 */
@EqualsAndHashCode(callSuper = true)
@Value
@Builder
@Serdeable
public class RunAnalysisCommandMessage extends PortalMessage {
    String datasetId;
    String projectId;
    String region;
    Map<String, String> environment;
    String fileAccessRoleArn;
    String datasetPath;
    String username;
}
