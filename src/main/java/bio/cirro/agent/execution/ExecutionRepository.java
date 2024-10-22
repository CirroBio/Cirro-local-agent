package bio.cirro.agent.execution;

import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@AllArgsConstructor
public class ExecutionRepository {
    private static final Map<String, ExecutionSession> sessionMap = new ConcurrentHashMap<>();

    public void add(ExecutionSession session) {
        sessionMap.put(session.getSessionId(), session);
    }

    public List<ExecutionSession> getAll() {
        return List.copyOf(sessionMap.values());
    }

    public ExecutionSession getSession(String sessionId) {
        var session = sessionMap.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid session ID");
        }
        return session;
    }

    public void removeSession(String sessionId) {
        sessionMap.remove(sessionId);
    }
}
