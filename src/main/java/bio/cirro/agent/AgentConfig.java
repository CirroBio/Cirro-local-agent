package bio.cirro.agent;

import bio.cirro.agent.socket.ConnectionInfo;
import io.micronaut.context.annotation.ConfigurationProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@ConfigurationProperties("cirro.agent")
@Setter
@Getter
public class AgentConfig {
    private String url;
    private String token;
    private String id;
    private int heartbeatInterval;
    private int watchInterval;
    private String logLevel;
    private String workDirectory;
    private String scriptsDirectory;
    private Path absoluteWorkDirectory;
    private Path absoluteScriptsDirectory;

    @PostConstruct
    public void init() {
        this.absoluteWorkDirectory = getAbsolutePath(workDirectory);
        this.absoluteScriptsDirectory = getAbsolutePath(scriptsDirectory);
    }

    public ConnectionInfo getConnectionInfo() {
        return ConnectionInfo.builder()
                .url(url)
                .token(token)
                .build();
    }

    public Duration watchInterval() {
        return Duration.ofSeconds(watchInterval);
    }

    public Duration heartbeatInterval() {
        return Duration.ofSeconds(heartbeatInterval);
    }

    private Path getAbsolutePath(String directory) {
        if (directory == null) {
            return null;
        }
        var expandedPath = directory.replaceFirst("^~", System.getProperty("user.home"));
        return Paths.get(expandedPath).toAbsolutePath();
    }
}
