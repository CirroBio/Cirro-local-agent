package bio.cirro.agent.socket;

import bio.cirro.agent.MessageHandlerFunction;
import bio.cirro.agent.utils.AWSRequestSigner;
import io.micronaut.context.annotation.Bean;
import io.micronaut.http.HttpRequest;
import io.micronaut.websocket.WebSocketClient;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * Sets up a WebSocket client for the agent to connect to the Cirro Portal.
 */
@Bean
@AllArgsConstructor
public class AgentClientFactory {
    private final WebSocketClient webSocketClient;
    private final AWSRequestSigner awsRequestSigner;

    public AgentClient connect(ConnectionInfo connectionInfo,
                               MessageHandlerFunction messageHandler) {
        var request = HttpRequest
                .GET(connectionInfo.url())
                .body("")
                .header("User-Agent", "Cirro Agent");

        var signedRequest = awsRequestSigner.signRequest(request);
        var client = webSocketClient.connect(AgentClient.class, signedRequest);
        var clientBlocking = Flux.from(client).blockFirst();

        assert clientBlocking != null;
        clientBlocking.setMessageHandler(messageHandler);
        return clientBlocking;
    }
}
