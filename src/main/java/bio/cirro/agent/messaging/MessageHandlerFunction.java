package bio.cirro.agent.messaging;

import bio.cirro.agent.messaging.dto.PortalMessage;

import java.util.Optional;
import java.util.function.Function;

/**
 * A function that handles a message and returns an optional response message.
 */
@FunctionalInterface
public interface MessageHandlerFunction extends Function<PortalMessage, Optional<PortalMessage>> {
}
