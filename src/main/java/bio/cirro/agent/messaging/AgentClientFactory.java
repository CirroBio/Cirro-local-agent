package bio.cirro.agent.messaging;

import bio.cirro.agent.aws.AwsRequestSigner;
import io.micronaut.context.annotation.Bean;
import io.micronaut.http.HttpRequest;
import io.micronaut.websocket.WebSocketClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import reactor.core.publisher.Flux;

/**
 * Sets up a WebSocket client for the agent to connect to the Cirro Portal.
 */
@Bean
@AllArgsConstructor
public class AgentClientFactory {
    private final WebSocketClient webSocketClient;
    private final AwsRequestSigner awsRequestSigner;

    @Getter
    private AgentClient clientSocket;

    public synchronized AgentClient connect(ConnectionInfo connectionInfo,
                                            MessageHandlerFunction messageHandler) {
        var request = HttpRequest
                .GET(connectionInfo.url() + "?agentId=" + connectionInfo.agentId())
                .body("")
                .header("User-Agent", connectionInfo.userAgent());

        var signedRequest = awsRequestSigner.signRequest(request, connectionInfo.region());
        var clientAsync = webSocketClient.connect(AgentClient.class, signedRequest);
        var client = Flux.from(clientAsync)
                .blockFirst();

        assert client != null;
        client.setMessageHandler(messageHandler);
        clientSocket = client;
        return client;
    }
}
