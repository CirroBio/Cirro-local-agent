package bio.cirro.agent.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micronaut.serde.annotation.Serdeable;

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

})
public abstract class PortalMessage {
}
