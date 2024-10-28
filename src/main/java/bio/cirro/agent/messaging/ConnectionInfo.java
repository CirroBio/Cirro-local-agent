package bio.cirro.agent.messaging;

import lombok.Builder;

@Builder
public record ConnectionInfo(
        String baseUrl,
        // Workaround for CloudFront not supporting IAM auth
        String tokenBaseUrl,
        String agentId,
        String userAgent,
        String region
) {

    String getTokenUrl() {
        return String.format("%s/api/agents/%s/ws-token", tokenBaseUrl, agentId);
    }

    String getWsUrl() {
        return String.format("%s/api/agents/%s/ws", baseUrl, agentId);
    }
}
