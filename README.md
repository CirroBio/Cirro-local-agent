# Cirro Agent

Cirro Agent is a daemon process that allows you to submit jobs to local compute resources.

## Running

Pre-requisites:
- Java 21 or higher

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
```

The following environment variables can be set to override the above configuration.

| Environment Variable           | Description                          |
|--------------------------------|--------------------------------------|
| CIRRO_AGENT_URL                | Path to the configuration file       |
| CIRRO_AGENT_ID                 | Agent ID                             |
| CIRRO_AGENT_WORK_DIRECTORY     | Working directory for jobs           |
| CIRRO_AGENT_SHARED_DIRECTORY   | Shared directory for jobs            |
| CIRRO_AGENT_HEARTBEAT_INTERVAL | Heartbeat interval in seconds        |
| CIRRO_AGENT_WATCH_INTERVAL     | Watch interval in seconds            |
| CIRRO_AGENT_LOG_LEVEL          | Log level (DEBUG, INFO, WARN, ERROR) |

### AWS Configuration

The agent will use the standard [AWS CLI environment variables](https://docs.aws.amazon.com/cli/v1/userguide/cli-configure-envvars.html) for configuration.

You can specify the `AWS_PROFILE` environment variable to use a specific profile.

### Directory and Scripts Setup

The agent requires a base directory to store working files and logs for workflows that are run.
By default, the agent will use the `work/` directory in the current working directory.

The agent also requires a shared directory for storing shared files, such as scripts.
By default, the agent will use the `shared/` directory in the current working directory.

You can specify a different directory by setting the relevant configuration or environment variable.

You must set up the following scripts:

- `submit_headnode.sh` (required)
   - This script is used to submit the headnode job to the local compute resource.
- `nextflow.config` (optional)
- `cromwell.conf` (optional)

Depending on your environment, you may also need supplementary scripts to support the above scripts.

We've included examples in the [`script-templates/`](./script-templates) directory for various runtime environments.

### Debugging

Debug mode can be enabled on the application by specifying the `--debug` flag on launch or setting log level to `DEBUG`.

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
