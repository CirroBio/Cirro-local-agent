package bio.cirro.agent.execution;

import jakarta.inject.Singleton;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ExecutionSessionService {
    private final Map<String, ExecutionSession> sessionMap = new ConcurrentHashMap<>();

    public void addSession(ExecutionSession session) {
        var sessionId = UUID.randomUUID().toString();
        sessionMap.put(sessionId, session);
    }

    public ExecutionSession getSession(String sessionId) {
        return sessionMap.get(sessionId);
    }
}
