package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.AgentTokenService;
import bio.cirro.agent.exception.ExecutionException;
import bio.cirro.agent.messaging.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.models.Status;
import bio.cirro.agent.utils.FileUtils;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Singleton
@AllArgsConstructor
@Slf4j
public class ExecutionCreateService {
    public static final String SUBMIT_SCRIPT = "submit_headnode.sh";
    private static final Pattern JOB_ID_REGEX = Pattern.compile("^\\d+$");

    private final AgentConfig agentConfig;
    private final AgentTokenService agentTokenService;
    private final ExecutionRepository executionRepository;
    private final EmbeddedServer embeddedServer;

    public Execution create(RunAnalysisCommandMessage runAnalysisCommandMessage) {
        var execution = Execution.builder()
                .messageData(runAnalysisCommandMessage)
                .agentWorkingDirectory(agentConfig.getAbsoluteWorkDirectory())
                .agentSharedDirectory(agentConfig.getAbsoluteSharedDirectory())
                .status(Status.PENDING)
                .createdAt(Instant.now())
                .build();
        executionRepository.add(execution);

        var token = agentTokenService.generateForExecution(execution.getDatasetId());
        try {
            // Set up working directory
            Files.createDirectories(execution.getWorkingDirectory());
            writeEnvironment(execution, token);
            writeAwsConfig(execution);

            var executionOutput = startExecution(execution);
            execution.setOutput(executionOutput);
        } catch (Exception ex) {
            executionRepository.remove(execution.getExecutionId());
            throw new ExecutionException("Failed to start execution", ex);
        }
        return execution;
    }

    private void writeEnvironment(Execution execution, String token) {
        var agentEndpoint = embeddedServer.getURI().toString();
        var environmentVariables = execution.getEnvironment(token, agentEndpoint);

        var environmentSb = new StringBuilder();
        environmentSb.append("#!/bin/bash\n");
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            environmentSb.append(String.format("export %s=%s%n", entry.getKey(), entry.getValue()));
        }

        var environmentFile = execution.getEnvironmentPath();
        try {
            FileUtils.writeScript(environmentFile, environmentSb.toString());
        } catch (IOException e) {
            throw new ExecutionException("Failed to write environment file", e);
        }
    }

    private void writeAwsConfig(Execution execution) {
        // Write AWS config file
        var awsConfigTemplate = FileUtils.getResourceAsString("aws-config.properties");

        try {
            Files.writeString(execution.getAwsConfigPath(), awsConfigTemplate);
        } catch (IOException e) {
            throw new ExecutionException("Failed to write AWS config", e);
        }

        // Write credential helper
        var credentialHelperScript = FileUtils.getResourceAsString("credential-helper.sh");
        try {
            FileUtils.writeScript(execution.getCredentialsHelperPath(), credentialHelperScript);
        } catch (IOException e) {
            throw new ExecutionException("Failed to write credential helper", e);
        }
    }

    private ExecutionStartOutput startExecution(Execution execution) {
        try {
            Path launchScript = Paths.get(agentConfig.getAbsoluteSharedDirectory().toString(), SUBMIT_SCRIPT);
            if (!launchScript.toFile().exists()) {
                throw new ExecutionException("Launch script not found", null);
                // TODO: Write default launch script?
            }
            log.debug("Using launch script: {}", launchScript);
            var headnodeLaunchProcessBuilder = new ProcessBuilder()
                    .directory(execution.getWorkingDirectory().toFile())
                    .command(launchScript.toAbsolutePath().toString())
                    .redirectErrorStream(true);
            // Set environment variables needed by the launch script
            var env = headnodeLaunchProcessBuilder.environment();
            env.put("PW_ENVIRONMENT_FILE", execution.getEnvironmentPath().toString());

            var headnodeLaunchProcess = headnodeLaunchProcessBuilder.start();

            StringBuilder processOutputSb = new StringBuilder();
            String jobId = null;
            try (var reader = new BufferedReader(new InputStreamReader(headnodeLaunchProcess.getInputStream()))) {
                for (String line : reader.lines().toList()) {
                    // Try to extract job ID
                    if (JOB_ID_REGEX.matcher(line).matches()) {
                        jobId = line;
                    }
                    processOutputSb.append(line).append("\n");
                }
            }
            var processOutput = processOutputSb.toString();
            log.debug("Execution output: {}", processOutput);
            if (jobId == null) {
                throw new ExecutionException("Failed to extract job ID");
            }

            headnodeLaunchProcess.waitFor(10, TimeUnit.SECONDS);

            if (headnodeLaunchProcess.exitValue() != 0) {
                throw new ExecutionException("Execution failed: " + processOutput);
            }
            headnodeLaunchProcess.destroy();
            return new ExecutionStartOutput(processOutput, jobId);
        } catch (IOException e) {
            throw new ExecutionException(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Thread interrupted", e);
        }
    }
}
