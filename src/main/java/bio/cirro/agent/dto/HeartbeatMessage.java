package bio.cirro.agent.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Message sent by the agent to the portal to indicate that the agent is still alive.
 * It does not contain any additional information.
 */
@EqualsAndHashCode(callSuper = true)
@Serdeable
@Value
public class HeartbeatMessage extends PortalMessage {
}
