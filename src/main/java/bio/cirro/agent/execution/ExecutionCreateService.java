package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.AgentTokenService;
import bio.cirro.agent.exception.ExecutionException;
import bio.cirro.agent.messaging.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.models.Status;
import bio.cirro.agent.utils.FileUtils;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Service to create and start an execution
 */
@Singleton
@AllArgsConstructor
@Slf4j
public class ExecutionCreateService {
    private static final Pattern JOB_ID_REGEX = Pattern.compile("^\\d+$");

    private final AgentConfig agentConfig;
    private final AgentTokenService agentTokenService;
    private final ExecutionRepository executionRepository;

    /**
     * Create and start an execution
     *
     * @param runAnalysisCommandMessage message containing details on the analysis to run
     * @return the created execution
     */
    public Execution create(RunAnalysisCommandMessage runAnalysisCommandMessage) {
        var execution = executionRepository.getNew(runAnalysisCommandMessage);
        executionRepository.add(execution);

        var token = agentTokenService.generateForExecution(execution.getDatasetId());
        try {
            // Set up working directory
            Files.createDirectories(execution.getWorkingDirectory());
            writeEnvironment(execution, token);
            writeAwsConfig(execution);
            log.info("Starting execution {} from {}", execution.getDatasetId(), execution.getUsername());
            var executionOutput = startExecution(execution);
            execution.setStartOutput(executionOutput);
        } catch (Exception ex) {
            execution.setStatus(Status.FAILED);
            throw new ExecutionException("Failed to start execution", ex);
        }
        executionRepository.update(execution);
        return execution;
    }

    /**
     * Write environment variables to a file
     */
    private void writeEnvironment(Execution execution, String token) {
        var agentEndpoint = agentConfig.getEndpoint();
        var environmentVariables = execution.getEnvironment(token, agentEndpoint);

        var environmentSb = new StringBuilder();
        environmentSb.append("#!/bin/bash\n");
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            environmentSb.append(String.format("export %s=\"%s\"%n", entry.getKey(), entry.getValue()));
        }

        var environmentFile = execution.getEnvironmentPath();
        try {
            FileUtils.writeScript(environmentFile, environmentSb.toString());
        } catch (IOException e) {
            throw new ExecutionException("Failed to write environment file", e);
        }
    }

    /*
     * Write AWS config file and credential helper script
     * @implNote The AWS config file has the full path to the credential helper script
     * because it cannot read from environment variables or relative paths
     */
    private void writeAwsConfig(Execution execution) {
        var awsConfigTemplate = FileUtils.getResourceAsString("aws-config.properties");
        var awsConfigFile = awsConfigTemplate
                .replace("%%CREDENTIAL_PROCESS_SCRIPT%%", execution.getCredentialsHelperPath().toString());
        try {
            Files.writeString(execution.getAwsConfigPath(), awsConfigFile);
        } catch (IOException e) {
            throw new ExecutionException("Failed to write AWS config", e);
        }

        var credentialHelperScript = FileUtils.getResourceAsString("credentials-helper.sh");
        try {
            FileUtils.writeScript(execution.getCredentialsHelperPath(), credentialHelperScript);
        } catch (IOException e) {
            throw new ExecutionException("Failed to write credential helper", e);
        }
    }

    /**
     * Submit the execution using the launch script
     * in the working directory of the execution
     */
    private ExecutionStartOutput startExecution(Execution execution) {
        try {
            Path submitScript = agentConfig.getSubmitScript();
            if (!submitScript.toFile().exists()) {
                throw new ExecutionException("Submit script not found", null);
            }
            log.debug("Using submit script: {}", submitScript);
            var headnodeLaunchProcessBuilder = new ProcessBuilder()
                    .directory(execution.getWorkingDirectory().toFile())
                    .command(submitScript.toAbsolutePath().toString())
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
