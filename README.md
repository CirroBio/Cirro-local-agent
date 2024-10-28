# Cirro Agent

Cirro Agent is a daemon process that allows you to submit jobs to local compute resources.

## Running

Pre-requisites:
- [Java 21 or higher](https://adoptium.net/)
- [AWS CLI configured](#aws-configuration)
- POSIX-compatible operating system (Linux, MacOS, WSL on Windows)

Download from the [Releases](https://github.com/CirroBio/Cirro-local-agent/releases) page and run the jar file:

```
java -jar cirro-agent-0.1-all.jar
```

## Configuration and Arguments

The agent can be configured through the `agent-config.yml` file in the working directory of the agent process.
You can also specify a different configuration file by using the `CIRRO_AGENT_CONFIG` environment variable.

The config file exposes the following options:

```yml
cirro:
  agent:
    url: https://app.cirro.bio/api
    id: default-agent
    work-directory: work/
    shared-directory: shared/
    # Advanced options
    heartbeat-interval: 30
    watch-interval: 2
    log-level: INFO
    jwt-secret: <RANDOM>
    jwt-expiry: 7
```

The following environment variables can be set to override the above configuration.

| Environment Variable           | Description                          | Default                                   |
|--------------------------------|--------------------------------------|-------------------------------------------|
| CIRRO_AGENT_URL                | Cirro instance URL                   | https://app.cirro.bio                     |
| CIRRO_AGENT_ID                 | Agent ID                             | default-agent                             |
| CIRRO_AGENT_WORK_DIRECTORY     | Working directory for jobs           | work/                                     |
| CIRRO_AGENT_SHARED_DIRECTORY   | Shared directory for jobs            | shared/                                   |
| CIRRO_AGENT_HEARTBEAT_INTERVAL | Heartbeat interval in seconds        | 60                                        |
| CIRRO_AGENT_WATCH_INTERVAL     | Watch interval in seconds            | 2                                         |
| CIRRO_AGENT_LOG_LEVEL          | Log level (DEBUG, INFO, WARN, ERROR) | INFO                                      |
| CIRRO_AGENT_JWT_SECRET         | JWT secret for signing               | Random value generated upon agent startup |
| CIRRO_AGENT_JWT_EXPIRY         | JWT expiry in days                   | 7                                         |

### AWS Configuration

The agent will use the standard [AWS CLI](https://docs.aws.amazon.com/cli/v1/userguide/cli-chap-configure.html) configuration for accessing AWS resources.

You can also use [environment variables](https://docs.aws.amazon.com/cli/v1/userguide/cli-configure-envvars.html), such as `AWS_PROFILE` to specify the profile to use.

The easiest way to set this up is to run `aws configure` and follow the prompts.

```bash
AWS_PROFILE="my-profile" java -jar cirro-agent-0.1-all.jar
```

### Directory and Scripts Setup

The agent requires a base directory to store working files and logs for workflows that are run.
By default, the agent will use the `work/` directory in the current working directory.

The agent also requires a shared directory for storing shared files, such as scripts.
By default, the agent will use the `shared/` directory in the current working directory.

You can specify a different directory by setting the relevant configuration or environment variable.

You must set up the following scripts in the shared directory:

- `submit_headnode.sh` (required)
  - This script is used to submit the headnode job to the local compute resource.
- `nextflow.local.config` (optional)
  - This file is used to set up the nextflow configuration for the job.
- `cromwell.local.config` (optional)
  - This file is used to set up the cromwell configuration for the job.

Depending on your environment, you may also need supplementary scripts to support the above scripts.

We've included examples in the [`script-templates/`](./script-templates) directory for various runtime environments.

If the job is executed on a different machine, you will need to ensure that these directories are shared between the machines, through a network share or a shared filesystem.

The work directory should be cleaned up periodically to remove old job files.
Files kept in here will be used to take advantage of workflow call caching for Nextflow and Cromwell.

### Agent Security

The jobs run by the agent communicate back through an HTTP server exposed by the agent.
The default is running on `http://localhost:8080`.
It is recommended to run the agent behind a reverse proxy with HTTPS enabled.
Each job is authenticated using a unique JWT token signed by the agent.
The default lifetime of the token is 7 days to account for long-running jobs. 

### Debugging

Debug mode can be enabled on the application by specifying the `--debug` flag on launch or setting log level to `DEBUG` in the configuration.

## Development

Run the agent using IntelliJ IDEA or run through the built jar file:

```
./gradlew shadowJar
java -jar build/libs/cirro-agent-0.1-all.jar
```

You can also build & run through the native executable:

```
./gradlew nativeCompile
./build/native/nativeCompile/cirro-agent
```
