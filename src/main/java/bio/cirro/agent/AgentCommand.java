package bio.cirro.agent;

import bio.cirro.agent.dto.AgentRegisterMessage;
import bio.cirro.agent.dto.HeartbeatMessage;
import bio.cirro.agent.exception.AgentException;
import bio.cirro.agent.socket.AgentClient;
import bio.cirro.agent.socket.AgentClientFactory;
import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
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
    private final TaskScheduler taskScheduler;
    private final MessageHandler messageHandler;
    private final AgentConfig agentConfig;
    private final LoggingSystem loggingSystem;

    @Option(names = {"-d", "--debug"}, description = "Enable debug logging", defaultValue = "${env:AGENT_DEBUG:-false}")
    boolean debugEnabled;

    // Internal state
    private AgentClient clientSocket;

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
        System.setProperty("micronaut.config.files", configFile);
        try (ApplicationContext context = ApplicationContext.builder(AgentCommand.class, Environment.CLI).start()) {
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
                clientSocket = agentClientFactory.connect(agentConfig.getConnectionInfo(), messageHandler::handleMessage);
                clientSocket.sendMessage(new AgentRegisterMessage(agentConfig.getId()));
            }
        } catch (WebSocketClientException e) {
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
            var workDirectory = agentConfig.getWorkDirectory();
            if (!Files.isDirectory(workDirectory)) {
                throw new AgentException("Working directory does not exist: " + workDirectory);
            }
            log.info("Using working directory: {}", workDirectory.toAbsolutePath());
        } catch (InvalidPathException e) {
            throw new AgentException("Working directory invalid: " + agentConfig.getWorkDirectory());
        }
    }

    /**
     * Changes the log level for the {@link bio.cirro.agent} package
     */
    private void setLogLevel() {
        var logLevel = debugEnabled ? LogLevel.DEBUG : LogLevel.valueOf(agentConfig.getLogLevel());
        loggingSystem.setLogLevel(AgentCommand.class.getPackageName(), logLevel);
    }
}
