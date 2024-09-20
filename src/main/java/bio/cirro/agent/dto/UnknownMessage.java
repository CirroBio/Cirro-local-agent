package bio.cirro.agent.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Messages that are not recognized by the agent.
 */
@EqualsAndHashCode(callSuper = true)
@Value
@Serdeable
public class UnknownMessage extends PortalMessage {
    String rawMessage;
}
