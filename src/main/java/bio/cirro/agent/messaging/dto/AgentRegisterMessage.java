package bio.cirro.agent.messaging.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Message sent by the agent to the portal to register itself
 * and provide the portal with details about the agent.
 */
@EqualsAndHashCode(callSuper = true)
@Serdeable
@Value
@Builder
public class AgentRegisterMessage extends PortalMessage {
    String agentId;
    String localIp;
    String hostname;
    String agentVersion;
    String os;
}
