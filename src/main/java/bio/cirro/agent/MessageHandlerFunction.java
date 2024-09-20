package bio.cirro.agent;

import bio.cirro.agent.dto.PortalMessage;

import java.util.Optional;
import java.util.function.Function;

/**
 * A function that handles a message and returns an optional response message.
 */
@FunctionalInterface
public interface MessageHandlerFunction extends Function<PortalMessage, Optional<PortalMessage>> {
}
