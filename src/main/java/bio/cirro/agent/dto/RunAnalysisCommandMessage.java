package bio.cirro.agent.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Map;

/**
 * Message sent by the portal to the agent to request the agent to run an analysis.
 */
@EqualsAndHashCode(callSuper = true)
@Value
@Serdeable
public class RunAnalysisCommandMessage extends PortalMessage {
    String datasetId;
    String projectId;
    String region;
    String workflow;
    String script;
    String version;
    String executor;
    Map<String, String> agentParameters;
    String parameterFilePath;
    String datasetPath;
    String githubToken;
}
