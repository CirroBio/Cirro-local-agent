# Cirro Agent

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=CirroBio_Cirro-local-agent&metric=alert_status&token=702d92a2776e625a3b4446454e190ad420a9d79a)](https://sonarcloud.io/summary/new_code?id=CirroBio_Cirro-local-agent)

Cirro Agent is a daemon process that allows you to submit jobs to local compute resources.

## Running

Pre-requisites:
- [Java 21 or higher](https://adoptium.net/)
- [AWS profile configured](#aws-configuration)
- POSIX-compatible operating system (Linux, macOS, WSL on Windows)

Download from the [Releases](https://github.com/CirroBio/Cirro-local-agent/releases) page and run the jar file:

```bash
java -jar cirro-agent-0.1-all.jar
```

Or, run the agent in a Docker container:

```bash
docker run \
  -e AWS_ACCESS_KEY_ID="<ACCESS_KEY>" \
  -e AWS_SECRET_ACCESS_KEY="<SECRET_KEY>" \
  -e AWS_REGION="<REGION>" \
  -e CIRRO_AGENT_URL="https://app.cirro.bio" \
  -e CIRRO_AGENT_ID="default-agent" \
  -e CIRRO_AGENT_ENDPOINT="http://servername:8080" \
  -v "${PWD}/work:/work" \
  -v "${PWD}/shared:/shared" \
  ghcr.io/cirrobio/cirro-agent:latest
```

You can also configure it by mapping the `agent-config.yml` file to the container `-v "${PWD}/agent-config.yml:/agent-config.yml"`.

## Configuration and Arguments

The agent can be configured through the `agent-config.yml` file in the working directory of the agent process.
You can also specify a different configuration file by using the `CIRRO_AGENT_CONFIG` environment variable.

The config file exposes the following options:

```yml
cirro:
  agent:
    url: https://app.cirro.bio
    id: default-agent
    endpoint: http://localhost:8080
    work-directory: work/
    shared-directory: shared/
    # Advanced options
    heartbeat-interval: 30
    watch-interval: 2
    log-level: INFO
    jwt-secret: <RANDOM>
    jwt-expiry: 7
    cleanup-threshold: 7
    submit-script-name: submit_headnode.sh
    stop-script-name: stop_headnode.sh
```

The following environment variables can be set to override the above configuration.

| Environment Variable           | Description                          | Default                                   |
|--------------------------------|--------------------------------------|-------------------------------------------|
| CIRRO_AGENT_URL                | Cirro instance URL                   | https://app.cirro.bio                     |
| CIRRO_AGENT_ID                 | Agent ID                             | default-agent                             |
| CIRRO_AGENT_ENDPOINT           | Agent endpoint URL                   | http://localhost:8080                     |
| CIRRO_AGENT_WORK_DIRECTORY     | Working directory for jobs           | work/                                     |
| CIRRO_AGENT_SHARED_DIRECTORY   | Shared directory for jobs            | shared/                                   |
| CIRRO_AGENT_HEARTBEAT_INTERVAL | Heartbeat interval in seconds        | 60                                        |
| CIRRO_AGENT_WATCH_INTERVAL     | Watch interval in seconds            | 2                                         |
| CIRRO_AGENT_LOG_LEVEL          | Log level (DEBUG, INFO, WARN, ERROR) | INFO                                      |
| CIRRO_AGENT_JWT_SECRET         | JWT secret for signing               | Random value generated upon agent startup |
| CIRRO_AGENT_JWT_EXPIRY         | JWT expiry in days                   | 7                                         |
| CIRRO_AGENT_CLEANUP_THRESHOLD  | Execution cleanup threshold in days  | 7                                         |

### AWS Configuration

The agent will use the standard [AWS CLI](https://docs.aws.amazon.com/cli/v1/userguide/cli-chap-configure.html) configuration for accessing AWS resources.

You can also use [environment variables](https://docs.aws.amazon.com/cli/v1/userguide/cli-configure-envvars.html), such as `AWS_PROFILE` to specify the profile to use.

The easiest way to set this up is to run `aws configure` and follow the prompts.

The IAM principal associated with the AWS CLI configuration must have the following permissions:

- `execute-api:Invoke` on `arn:aws:execute-api:<REGION>:<CIRRO_ACCOUNT_ID>:*`
- `sts:AssumeRole` on `arn:aws:iam::<PROJECT_ACCOUNT_ID>:role/Cirro-LocalAgentRole-*`

A sample policy is available at [agent-policy.json](./agent-policy.json).

### Directory and Scripts Setup

The agent requires a base directory to store working files and logs for workflows that are run.
By default, the agent will use the `work/` directory in the current working directory.
Projects will be separated by subdirectories within this directory.

The agent also requires a shared directory for storing shared files, such as scripts.
By default, the agent will use the `shared/` directory in the current working directory.

You can specify a different directory by setting the relevant configuration or environment variable.

You must set up the following scripts in the shared directory:

- `submit_headnode.sh` (required)
  - This script is used to submit the headnode job to the local compute resource.
- `stop_headnode.sh` (required)
  - This script is used to stop the headnode job on the local compute resource.
- `nextflow.local.config` (optional)
  - This file is used to set up the nextflow configuration for the job.
- `cromwell.local.config` (optional)
  - This file is used to set up the cromwell configuration for the job.

Depending on your environment, you may also need scripts to supplement the above scripts.

We've included examples in the [`script-templates/`](./script-templates) directory for various runtime environments.

If the job is executed on a different machine, you will need to ensure that these directories are shared between the machines, through a network share or a shared filesystem.

The work directory should be cleaned up periodically to remove old job files.
Files kept in here will be used to take advantage of workflow call caching for Nextflow and Cromwell.

### Agent Security

The jobs run by the agent communicate back through an HTTP server exposed by the agent.
The default endpoint is exposed at `http://localhost:8080`.
It is recommended to run the agent behind a reverse proxy with HTTPS enabled.

Each job is authenticated to this endpoint using a unique JWT token signed by the agent.
The default lifetime of the token is 7 days to account for long-running jobs.

When running behind a reverse proxy, change the endpoint configuration property to the public URL of the agent.

### Agent Persistence

By default, the agent will store its execution state in memory. 
If the agent is restarted, it will lose all running execution information.
To persist the agent state across restarts, you can configure to use a file-based database.

To enable persistence, add the following configuration to the `agent-config.yml` file:

```yml
datasources:
  default:
    url: "jdbc:h2:file:./cirro-agent;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
    schema-generate: CREATE
```

This will create a file in the working directory called `cirro-agent.mv.db`.
The first time the agent is started with this configuration, it will create the database schema. 
After that, you can change the `schema-generate` property to `NONE`.

Completed executions will be removed from the database after the configured `cleanup-threshold` days.

### Debugging

Debug mode can be enabled on the application by specifying the `--debug` flag on launch or setting log level to `DEBUG` in the configuration.

### Logging

The agent logs to the console (stdout) by default.

You can also override the default log configuration by providing a custom Logback configuration file and specifying the `logger.config` property in your configuration.

```
logger:
  config: logback-override.xml
```

A sample Logback configuration file that writes to both the console and a log file is available at [logback-override.xml](./logback-override.xml).
The log file is configured to write to `agent.log`, rotate daily, and keep 7 days of logs.

## Development

Run the agent using IntelliJ IDEA or run through the built jar file:

```bash
./gradlew shadowJar
java -jar build/libs/cirro-agent-0.1-all.jar
```

You can also build & run through the native executable:

```bash
./gradlew nativeCompile
./build/native/nativeCompile/cirro-agent
```
