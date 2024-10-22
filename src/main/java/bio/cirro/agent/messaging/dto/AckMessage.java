package bio.cirro.agent.messaging.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Serdeable
@Value
@EqualsAndHashCode(callSuper = true)
public class AckMessage extends PortalMessage {
    String message;
}
