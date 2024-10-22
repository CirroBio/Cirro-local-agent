package bio.cirro.agent.messaging.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Base class for all messages that can be sent to the portal.
 * Uses the type field to determine the concrete class to deserialize to.
 * <p>
 * Messages that are not recognized will be deserialized to {@link UnknownMessage}.
 */
@Serdeable
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        visible = true,
        defaultImpl = UnknownMessage.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HeartbeatMessage.class, name = "heartbeat"),
        @JsonSubTypes.Type(value = RunAnalysisCommandMessage.class, name = "run-analysis"),
        @JsonSubTypes.Type(value = AgentRegisterMessage.class, name = "register"),
        @JsonSubTypes.Type(value = RunAnalysisResponseMessage.class, name = "run-analysis-response"),
        @JsonSubTypes.Type(value = AckMessage.class, name = "ack"),
})
public abstract class PortalMessage {
}
