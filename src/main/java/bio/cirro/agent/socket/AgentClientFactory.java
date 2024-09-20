package bio.cirro.agent.socket;

import bio.cirro.agent.MessageHandlerFunction;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.http.HttpRequest;
import io.micronaut.websocket.WebSocketClient;
import reactor.core.publisher.Flux;

@Bean
public class AgentClientFactory {
    WebSocketClient webSocketClient;

    public AgentClientFactory(ApplicationContext applicationContext) {
        this.webSocketClient = applicationContext.getBean(WebSocketClient.class);
    }

    public AgentClient connect(ConnectionInfo connectionInfo,
                               MessageHandlerFunction messageHandler) {
        var request = HttpRequest
                .GET(connectionInfo.url())
                .header("User-Agent", "Cirro Agent")
                .bearerAuth(connectionInfo.token());

        var client = webSocketClient.connect(AgentClient.class, request);
        var clientBlocking = Flux.from(client).blockFirst();

        assert clientBlocking != null;
        clientBlocking.setMessageHandler(messageHandler);
        return clientBlocking;
    }
}
