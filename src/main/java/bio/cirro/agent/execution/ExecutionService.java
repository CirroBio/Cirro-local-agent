package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.client.FileClient;
import bio.cirro.agent.client.TokenClient;
import bio.cirro.agent.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.exception.ExecutionException;
import bio.cirro.agent.models.AWSCredentials;
import bio.cirro.agent.utils.FileUtils;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
@AllArgsConstructor
@Slf4j
public class ExecutionService {
    private static final Pattern JOB_ID_REGEX = Pattern.compile(".*Job ID: (\\d+).*");

    private final AgentConfig agentConfig;
    private final TokenClient tokenClient;
    private final ExecutionRepository executionRepository;

    public ExecutionSession createSession(RunAnalysisCommandMessage runAnalysisCommandMessage) {
        var sessionId = UUID.randomUUID().toString();
        var workingDirectory = Paths.get(agentConfig.getAbsoluteWorkDirectory().toString(), sessionId);
        var session = ExecutionSession.builder()
                .sessionId(sessionId)
                .datasetId(runAnalysisCommandMessage.getDatasetId())
                .projectId(runAnalysisCommandMessage.getProjectId())
                .datasetS3(runAnalysisCommandMessage.getDatasetPath())
                .workingDirectory(workingDirectory)
                .build();
        executionRepository.add(session);

        var creds = tokenClient.generateCredentialsForExecutionSession(session);
        session.setAwsCredentials(creds);

        try (var fileClient = new FileClient(session.getAwsCredentialsProvider())) {
            // Set up working directory
            writeParams(session, fileClient);
            writeConfig(session);
            writeEnvironment(session);
            writeAwsConfig(session);

            var executionOutput = startExecution(session);
            session.setOutput(executionOutput);
        } catch (Exception ex) {
            executionRepository.removeSession(sessionId);
            throw new ExecutionException("Failed to start execution", ex);
        }
        return session;
    }

    public AWSCredentials generateExecutionS3Credentials(String sessionId) {
        var session = executionRepository.getSession(sessionId);
        var creds = tokenClient.generateCredentialsForExecutionSession(session);
        return AWSCredentials.builder()
                .accessKeyId(creds.accessKeyId())
                .secretAccessKey(creds.secretAccessKey())
                .sessionToken(creds.sessionToken())
                .expiration(creds.expirationTime().orElse(null))
                .build();
    }

    public void completeExecution(String sessionId) {
        // Handle stuff
        // Remove if everything is successful
        executionRepository.removeSession(sessionId);
    }

    private void writeParams(ExecutionSession session, FileClient fileClient) {
        try {
            var configPath = session.getDatasetS3Path().resolve("config/params.json");
            var params = fileClient.getObject(configPath);
            Files.writeString(session.getParamsPath(), params);
        } catch (IOException e) {
            throw new ExecutionException("Failed to download params file", e);
        }

    }

    private void writeConfig(ExecutionSession session) {
        StringBuilder configSb = new StringBuilder();
        configSb.append(String.format("workDir = %s%n", session.getWorkingDirectory().toString()));

        try {
            Files.writeString(session.getNextflowConfigPath(), configSb.toString());
        } catch (IOException e) {
            throw new ExecutionException("Failed to write config file", e);
        }
    }

    private Map<String, String> generateEnvironment(ExecutionSession session) {
        return Map.ofEntries(
                Map.entry("DATASET_ID", session.getDatasetId()),
                Map.entry("AWS_CONFIG_FILE", session.getAwsConfigPath().toString()),
                Map.entry("AWS_SHARED_CREDENTIALS_FILE", session.getAwsCredentialsPath().toString())
        );
    }

    private void writeEnvironment(ExecutionSession session) {
        var environmentVariables = generateEnvironment(session);

        var environmentSb = new StringBuilder();
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            environmentSb.append(String.format("export %s=%s%n", entry.getKey(), entry.getValue()));
        }

        var environmentFile = session.getEnvironmentPath();
        try {
            FileUtils.writeScript(environmentFile, environmentSb.toString());
        } catch (IOException e) {
            throw new ExecutionException("Failed to write environment file", e);
        }
    }

    private void writeAwsConfig(ExecutionSession session) {
        // Write AWS config file
        var awsConfigTemplate = FileUtils.getResourceAsString("aws-config.properties");
        awsConfigTemplate = awsConfigTemplate.replace("%%SESSION_ID%%", session.getSessionId());

        try {
            Files.writeString(session.getAwsConfigPath(), awsConfigTemplate);
        } catch (IOException e) {
            throw new ExecutionException("Failed to write AWS config", e);
        }

        // Write credential helper
        var credentialHelperScript = FileUtils.getResourceAsString("credential-helper.sh");
        try {
            FileUtils.writeScript(session.getCredentialsHelperPath(), credentialHelperScript);
        } catch (IOException e) {
            throw new ExecutionException("Failed to write credential helper", e);
        }
    }

    private ExecutionSessionOutput startExecution(ExecutionSession session) {
        try {
            // Headnode launch script
            // pass working directory, user-provided

            // Headnode execution script
            // Load "Cirro" headnode script
            // Load user-defined script
            // str.replace("## TEMPLATE", user-defined script)
            // Write to working directory

            Path launchScript = Paths.get(agentConfig.getAbsoluteScriptsDirectory().toString(), "submit_headnode.sh");
            if (!launchScript.toFile().exists()) {
                throw new ExecutionException("Launch script not found", null);
                // TODO: Write default launch script?
            }
            log.debug("Using launch script: {}", launchScript);
            var headnodeLaunchProcessBuilder = new ProcessBuilder()
                    .directory(session.getWorkingDirectory().toFile())
                    .command("sh", launchScript.toAbsolutePath().toString())
                    .redirectErrorStream(true);
            // Set environment variables
            var env = headnodeLaunchProcessBuilder.environment();
            env.put("PROCESS_DIR", session.getWorkingDirectory().toString());
            env.put("PROCESS_GUID", session.getDatasetId());
            env.putAll(generateEnvironment(session));

            var headnodeLaunchProcess = headnodeLaunchProcessBuilder.start();

            String processOutput;
            try (var reader = new BufferedReader(new InputStreamReader(headnodeLaunchProcess.getInputStream()))) {
                processOutput = reader.lines().collect(Collectors.joining("\n"));
            }
            headnodeLaunchProcess.waitFor(10, TimeUnit.SECONDS);

            if (headnodeLaunchProcess.exitValue() != 0) {
                throw new ExecutionException("Execution failed: " + processOutput);
            }
            headnodeLaunchProcess.destroy();
            log.debug("Execution output: {}", processOutput);

            String jobId = null;
            var matcher = JOB_ID_REGEX.matcher(processOutput);
            if (matcher.find()) {
                jobId = matcher.group(1);
            }
            return new ExecutionSessionOutput(jobId, processOutput);
        } catch (IOException e) {
            throw new ExecutionException(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Thread interrupted", e);
        }
    }
}
