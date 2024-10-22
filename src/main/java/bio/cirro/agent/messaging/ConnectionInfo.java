package bio.cirro.agent.messaging;

import lombok.Builder;

@Builder
public record ConnectionInfo(
        String url,
        String agentId,
        String userAgent,
        String region
) {
}
