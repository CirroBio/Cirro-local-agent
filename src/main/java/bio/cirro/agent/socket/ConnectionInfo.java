package bio.cirro.agent.socket;

import lombok.Builder;

@Builder
public record ConnectionInfo(
        String url,
        String agentId
) {
}
