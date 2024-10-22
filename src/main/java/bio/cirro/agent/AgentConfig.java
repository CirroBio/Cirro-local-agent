package bio.cirro.agent;

import bio.cirro.agent.utils.SystemUtils;
import io.micronaut.context.annotation.ConfigurationProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@ConfigurationProperties("cirro.agent")
@Setter
@Getter
@Slf4j
public class AgentConfig {
    private String url;
    private String id;
    private int heartbeatInterval;
    private int watchInterval;
    private String logLevel;
    private String workDirectory;
    private String sharedDirectory;
    private Path absoluteWorkDirectory;
    private Path absoluteSharedDirectory;
    private String version;
    private String fileAccessRoleArn;
    private byte[] jwtSecret;

    @PostConstruct
    public void init() {
        this.absoluteWorkDirectory = getAbsolutePath(workDirectory);
        this.absoluteSharedDirectory = getAbsolutePath(sharedDirectory);
        if (this.jwtSecret == null) {
            log.debug("Generating random JWT secret");
            this.jwtSecret = SystemUtils.generateRandomBytes(20);
        }
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
