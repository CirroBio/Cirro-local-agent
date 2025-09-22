package bio.cirro.agent;

import bio.cirro.agent.exception.AgentException;
import bio.cirro.agent.execution.ExecutionCleanupService;
import bio.cirro.agent.messaging.AgentClientFactory;
import bio.cirro.agent.messaging.ConnectionInfo;
import bio.cirro.agent.messaging.dto.AgentRegisterMessage;
import bio.cirro.agent.messaging.dto.HeartbeatMessage;
import bio.cirro.agent.utils.FileUtils;
import bio.cirro.agent.utils.SystemUtils;
import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.logging.LogLevel;
import io.micronaut.logging.LoggingSystem;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.websocket.exceptions.WebSocketClientException;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
    private final ApplicationContext applicationContext;
    private final AgentClientFactory agentClientFactory;
    private final HttpClient httpClient;
    private final TaskScheduler taskScheduler;
    private final MessageHandler messageHandler;
    private final ExecutionCleanupService executionCleanupService;
    private final AgentConfig agentConfig;
    private final LoggingSystem loggingSystem;

    @Option(names = {"-d", "--debug"}, description = "Enable debug logging", defaultValue = "${env:AGENT_DEBUG:-false}")
    boolean debugEnabled;

    // Internal state
    private ConnectionInfo connectionInfo;

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
            connectionInfo = agentClientFactory.buildConnectionInfo(agentConfig);

            applicationContext
                    .findBean(EmbeddedServer.class)
                    .ifPresent(server -> {
                        server.start();
                        log.debug("Embedded server started at {}", server.getURI());
                    });

            // Schedule connection watcher and heartbeat tasks
            var watcher = taskScheduler.scheduleAtFixedRate(Duration.ZERO, agentConfig.watchInterval(), this::watchAndInitConnection);
            taskScheduler.scheduleAtFixedRate(agentConfig.heartbeatInterval(), agentConfig.heartbeatInterval(), this::sendHeartbeat);
            taskScheduler.scheduleAtFixedRate(Duration.ofSeconds(1), Duration.ofDays(1), executionCleanupService::cleanupOldExecutions);
            // Wait for the watcher task to complete (it only completes when an exception is thrown)
            watcher.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted", e);
            System.exit(1);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof AgentException) {
                log.error(e.getCause().getMessage());
            } else {
                log.error("Unexpected error", e);
            }
            System.exit(1);
        } catch (AgentException e) {
            log.error(e.getMessage());
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
        var clientSocket = agentClientFactory.getClientSocket();
        try {
            if (clientSocket == null || !clientSocket.isOpen()) {
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
        }
        // Only throw an exception if the first attempt to connect has failed
        // (clientSocket will be null) Exceptions will cause the agent to exit
        catch (WebSocketClientException e) {
            var msg = e.getMessage();
            if (clientSocket == null) {
                throw new AgentException("Failed to connect: " + msg);
            }
            log.error(e.getMessage());
        }
        catch (HttpClientException e) {
            if (clientSocket == null) {
                var status = "Error";
                if (e instanceof HttpClientResponseException responseException) {
                    status = responseException.getStatus().getReason();
                }
                throw new AgentException(String.format("%s: %s", status, e.getMessage()));
            }
            log.error(e.getMessage());
        }
    }

    /**
     * Send a heartbeat message to the server to avoid disconnection
     */
    private void sendHeartbeat() {
        var clientSocket = agentClientFactory.getClientSocket();
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

        FileUtils.validateDirectory(agentConfig.getAbsoluteWorkDirectory(), "Work");
        FileUtils.validateDirectory(agentConfig.getAbsoluteSharedDirectory(), "Shared");
        var submitScript = agentConfig.getSubmitScript();
        if (!Files.exists(submitScript)) {
            throw new AgentException(String.format("Submit script (%s) not found", submitScript));
        }
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
