package bio.cirro.agent.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Message sent by the agent to the portal to register itself
 * and provide the portal with details about the agent.
 */
@EqualsAndHashCode(callSuper = true)
@Serdeable
@Value
public class AgentRegisterMessage extends PortalMessage {
    String agentId;
}
