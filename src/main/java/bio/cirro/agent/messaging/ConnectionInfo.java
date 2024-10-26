package bio.cirro.agent.messaging;

import lombok.Builder;

@Builder
public record ConnectionInfo(
        String url,
        String wsUrl,
        String agentId,
        String userAgent,
        String region
) {
}
