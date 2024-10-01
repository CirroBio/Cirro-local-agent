package bio.cirro.agent;

import bio.cirro.agent.dto.AgentRegisterMessage;
import bio.cirro.agent.dto.HeartbeatMessage;
import bio.cirro.agent.exception.AgentException;
import bio.cirro.agent.models.SystemInfoResponse;
import bio.cirro.agent.socket.AgentClient;
import bio.cirro.agent.socket.AgentClientFactory;
import bio.cirro.agent.socket.ConnectionInfo;
import bio.cirro.agent.utils.SystemUtils;
import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.logging.LogLevel;
import io.micronaut.logging.LoggingSystem;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.websocket.exceptions.WebSocketClientException;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;

/**
 * Cirro Agent entry point.
 */
@Command(
        name = "cirro-agent",
        mixinStandardHelpOptions = true
)
@Singleton
@Slf4j
@RequiredArgsConstructor
public class AgentCommand implements Runnable {
    // Injected dependencies
    private final AgentClientFactory agentClientFactory;
    private final HttpClient httpClient;
    private final TaskScheduler taskScheduler;
    private final MessageHandler messageHandler;
    private final AgentConfig agentConfig;
    private final LoggingSystem loggingSystem;

    @Option(names = {"-d", "--debug"}, description = "Enable debug logging", defaultValue = "${env:AGENT_DEBUG:-false}")
    boolean debugEnabled;

    // Internal state
    private AgentClient clientSocket;
    private SystemInfoResponse systemInfo;

    public static void main(String[] args) {
        int exitCode = execute(args);
        System.exit(exitCode);
    }

    /**
     * Set up the Application Context and command line.
     * <p>
     * This is not technically required as one can just use PicocliRunner
     * in the main method, but this provides additional customization options.
     */
    private static int execute(String[] args) {
        var configFile = Optional.ofNullable(System.getenv("CIRRO_AGENT_CONFIG"))
                .orElse("./agent-config.yml");
        if (Files.exists(Paths.get(configFile))) {
            System.setProperty("micronaut.config.files", configFile);
        }
        try (ApplicationContext context = ApplicationContext.builder(AgentCommand.class, Environment.CLI).banner(false).start()) {
            CommandLine cmd = new CommandLine(AgentCommand.class, new MicronautFactory(context))
                    .setCaseInsensitiveEnumValuesAllowed(true)
                    .setUsageHelpAutoWidth(true);
            // Print header with ANSI colors on all cases (not just help text)
            var header = new String[]{
                    "@|green  __     __   __   __ |@",
                    "@|green /  ` | |__) |__) /  \\|@",
                    "@|green \\__, | |  \\ |  \\ \\__/|@",
                    "@|green                       |@",
                    "@|yellow Cirro Agent version 0.1.0 |@"
            };
            for (String line : header) {
                System.out.println(CommandLine.Help.Ansi.AUTO.string(line));
            }
            return cmd.execute(args);
        }
    }

    /**
     * Main agent loop
     * Gets called once the CLI framework is done (i.e., after parsing the command line arguments).
     */
    @Override
    public void run() {
        try {
            setLogLevel();
            validateParams();
            systemInfo = connectCirro();

            // Schedule connection watcher and heartbeat tasks
            var watcher = taskScheduler.scheduleAtFixedRate(Duration.ZERO, agentConfig.watchInterval(), this::watchAndInitConnection);
            taskScheduler.scheduleAtFixedRate(agentConfig.heartbeatInterval(), agentConfig.heartbeatInterval(), this::sendHeartbeat);

            // Wait for the watcher task to complete (it only completes when an exception is thrown)
            watcher.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted", e);
            System.exit(1);
        } catch (AgentException e) {
            log.error("Error: {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            System.exit(1);
        }
    }

    /**
     * Initialize the connection to the server if it is not already open
     */
    private void watchAndInitConnection() {
        try {
            if (clientSocket == null || !clientSocket.isOpen()) {
                var connectionInfo = ConnectionInfo.builder()
                        .url(systemInfo.agentEndpoint())
                        .region(systemInfo.region())
                        .agentId(agentConfig.getId())
                        .userAgent(agentConfig.getUserAgent())
                        .build();
                clientSocket = agentClientFactory.connect(connectionInfo, messageHandler::handleMessage);
                var versionString = String.format("Cirro Agent (%s) on %s", agentConfig.getVersion(), SystemUtils.getJavaVersion());
                clientSocket.sendMessage(
                        AgentRegisterMessage.builder()
                                .agentId(agentConfig.getId())
                                .os(SystemUtils.getOs())
                                .agentVersion(versionString)
                                .localIp(SystemUtils.getLocalIp())
                                .hostname(SystemUtils.getHostname())
                                .build()
                );
            }
        } catch (WebSocketClientException e) {
            var msg = e.getMessage();
            if (msg.contains(HttpStatus.BAD_REQUEST.getReason())) {
                throw new AgentException("Bad Request: invalid connection info, check agent ID");
            }
            if (msg.contains(HttpStatus.UNAUTHORIZED.getReason())) {
                throw new AgentException("Unauthorized: check that the role has access");
            }
            log.error(e.getMessage());
        }
    }

    /**
     * Send a heartbeat message to the server to avoid disconnection
     */
    private void sendHeartbeat() {
        if (clientSocket != null && clientSocket.isOpen()) {
            clientSocket.sendMessage(new HeartbeatMessage());
        }
    }

    /**
     * Validate required parameters
     */
    private void validateParams() {
        if (agentConfig.getUrl() == null || agentConfig.getUrl().isBlank()) {
            throw new AgentException("URL is required");
        }

        try {
            var workDirectory = agentConfig.getAbsoluteWorkDirectory();
            if (!Files.isDirectory(workDirectory)) {
                throw new AgentException("Working directory does not exist: " + workDirectory);
            }
            log.info("Using working directory: {}", workDirectory);
        } catch (InvalidPathException e) {
            throw new AgentException("Working directory invalid: " + agentConfig.getWorkDirectory());
        }
    }

    private SystemInfoResponse connectCirro() {
        var url = agentConfig.getUrl() + "/api/info/system";
        log.debug("Connecting to Cirro at {}", url);
        var request = HttpRequest.GET(url)
                .header("User-Agent", agentConfig.getUserAgent())
                .accept("application/json");
        var response = httpClient.toBlocking().retrieve(request, SystemInfoResponse.class);
        if (response.agentEndpoint() == null || response.agentEndpoint().isBlank()) {
            throw new AgentException("Invalid Cirro server response");
        }
        return response;
    }

    /**
     * Changes the log level for the {@link bio.cirro.agent} package
     */
    private void setLogLevel() {
        var logLevel = debugEnabled ? LogLevel.DEBUG : LogLevel.valueOf(agentConfig.getLogLevel());
        loggingSystem.setLogLevel(AgentCommand.class.getPackageName(), logLevel);
        log.debug("Setting log level to {}", logLevel);
    }
}
