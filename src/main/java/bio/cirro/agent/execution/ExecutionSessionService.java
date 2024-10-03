package bio.cirro.agent.execution;

import bio.cirro.agent.dto.AgentRegisterMessage;
import bio.cirro.agent.models.AWSCredentials;
import bio.cirro.agent.models.ExecutionSession;
import bio.cirro.agent.utils.TokenClient;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@AllArgsConstructor
public class ExecutionSessionService {
    private final TokenClient tokenClient;

    private final Map<String, ExecutionSession> sessionMap = new ConcurrentHashMap<>();

    public void createSession(AgentRegisterMessage registerMessage) {
        var sessionId = UUID.randomUUID().toString();
        var session = ExecutionSession.builder()
                        .build();
        sessionMap.put(sessionId, session);
    }

    public ExecutionSession getSession(String sessionId) {
        return sessionMap.get(sessionId);
    }

    public AWSCredentials generateExecutionS3Credentials(String sessionId) {
        var session = getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid session ID");
        }
        return tokenClient.generateCredentialsForExecutionSession(session);
    }

    public void completeExecution(String sessionId) {
        sessionMap.remove(sessionId);
    }
}
