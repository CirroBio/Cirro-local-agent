package bio.cirro.agent;

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
    private String version;
    private String fileAccessRoleArn;

    @PostConstruct
    public void init() {
        this.absoluteWorkDirectory = getAbsolutePath(workDirectory);
        this.absoluteScriptsDirectory = getAbsolutePath(scriptsDirectory);
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

    public String getUserAgent() {
        return "Cirro Agent/" + version;
    }
}
