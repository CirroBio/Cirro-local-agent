package bio.cirro.agent;

import bio.cirro.agent.dto.PortalMessage;

import java.util.Optional;
import java.util.function.Function;

@FunctionalInterface
public interface MessageHandlerFunction extends Function<PortalMessage, Optional<PortalMessage>> {
}
