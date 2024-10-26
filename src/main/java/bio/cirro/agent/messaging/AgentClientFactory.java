package bio.cirro.agent.messaging;

import bio.cirro.agent.aws.AwsRequestSigner;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.websocket.WebSocketClient;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Instant;


/**
 * Sets up a WebSocket client for the agent to connect to the Cirro Portal.
 */
@Singleton
@RequiredArgsConstructor
@Slf4j
public class AgentClientFactory {
    private final WebSocketClient webSocketClient;
    private final AwsRequestSigner awsRequestSigner;
    private final HttpClient httpClient;

    @Getter
    private AgentClient clientSocket;
    private DecodedJWT jwt;
    private String wsConnectionUrl;

    public synchronized AgentClient connect(ConnectionInfo connectionInfo,
                                            MessageHandlerFunction messageHandler) {
        var token = getToken(connectionInfo);
        var request = HttpRequest
                .GET(String.format("%s?agentId%s", wsConnectionUrl, connectionInfo.agentId()))
                .body("")
                .header("User-Agent", connectionInfo.userAgent())
                .header("Authorization", "Bearer " + token.getToken());

        var clientAsync = webSocketClient.connect(AgentClient.class, request);
        var client = Flux.from(clientAsync)
                .blockFirst();

        assert client != null;
        client.setMessageHandler(messageHandler);
        clientSocket = client;
        return client;
    }

    private DecodedJWT getToken(ConnectionInfo connectionInfo) {
        if (jwt != null && !jwt.getExpiresAtAsInstant().isBefore(Instant.now())) {
            log.debug("Using cached JWT token");
            return jwt;
        }
        log.debug("Fetching new JWT token");
        var request = HttpRequest
                .POST(String.format("%s/api/agents/%s/ws-token", connectionInfo.tokenUrl(), connectionInfo.agentId()), null)
                .body("")
                .header("User-Agent", connectionInfo.userAgent());
        var signedRequest = awsRequestSigner.signRequest(request, connectionInfo.region());
        var resp = httpClient.toBlocking().retrieve(signedRequest, Argument.mapOf(String.class, String.class));
        var tokenRaw = resp.get("token");
        jwt = JWT.decode(tokenRaw);
        wsConnectionUrl = resp.get("wsUrl");
        return jwt;
    }
}
