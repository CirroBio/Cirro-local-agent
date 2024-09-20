package bio.cirro.agent.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Serdeable
@Value
public class AgentRegisterMessage extends PortalMessage {
    String agentId;
}
