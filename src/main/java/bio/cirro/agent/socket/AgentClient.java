package bio.cirro.agent.socket;

import bio.cirro.agent.MessageHandlerFunction;
import bio.cirro.agent.dto.PortalMessage;
import bio.cirro.agent.dto.UnknownMessage;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.ClientWebSocket;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;


/**
 * A WebSocket client that interfaces with the Cirro Portal.
 */
@ClientWebSocket
@Slf4j
@RequiredArgsConstructor
public abstract class AgentClient implements AutoCloseable {
    // Injected dependencies
    private final ObjectMapper objectMapper;

    private WebSocketSession session;

    @Setter
    private MessageHandlerFunction messageHandler;

    @OnOpen
    public void onOpen(WebSocketSession session) {
        this.session = session;
        log.info("Connected to Cirro");
        log.debug("Session ID: {}", session.getId());
        log.debug("URI: {}", session.getRequestURI());
    }

    @OnClose
    public void onClose(CloseReason reason) {
        log.info("Disconnected from {}", reason);
    }

    @OnMessage
    public void onMessage(String message) {
        log.debug("Received message raw: {}", message);
        PortalMessage portalMessage;
        try {
            portalMessage = objectMapper.readValue(message, PortalMessage.class);
        } catch (IOException e) {
            portalMessage = new UnknownMessage(message);
        }
        Optional<PortalMessage> response = messageHandler.apply(portalMessage);
        response.ifPresent(this::sendMessage);
    }

    public void sendMessage(PortalMessage message) {
        try {
            var json = objectMapper.writeValueAsString(message);
            log.debug("Sending message raw: {}", json);
            session.sendSync(json);
        } catch (IOException e) {
            log.error("Failed to serialize message", e);
        }
    }

    /**
     * Is the WebSocket connection open?
     */
    public boolean isOpen() {
        return session.isOpen();
    }
}
