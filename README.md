## Cirro Agent

Cirro Agent is a daemon process that allows you to submit jobs to local compute resources.


## Running

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

Arguments:

## Configuration and Arguments

The agent can be configured through the `agent-config.yml` file in the current working directory.
You can specify a different configuration file by using the `CIRRO_AGENT_CONFIG` environment variable.

The config file exposes the following options:

```yml
cirro:
  agent:
    url: https://app.cirro.bio/api
    id: default-agent
    token: <API Token>
    work-directory: work/
    # Advanced options
    heartbeat-interval: 30
    watch-interval: 2
    log-level: INFO
```

The following environment variables can be set to override the above configuration.
We recommend setting `CIRRO_AGENT_TOKEN` to avoid putting secrets into a file.

| Environment Variable           | Description                          |
|--------------------------------|--------------------------------------|
| CIRRO_AGENT_URL                | Path to the configuration file       |
| CIRRO_AGENT_ID                 | Agent ID                             |
| CIRRO_AGENT_HEARTBEAT_INTERVAL | Heartbeat interval in seconds        |
| CIRRO_AGENT_WATCH_INTERVAL     | Watch interval in seconds            |
| CIRRO_AGENT_TOKEN              | API token                            |
| CIRRO_AGENT_LOG_LEVEL          | Log level (DEBUG, INFO, WARN, ERROR) |
| CIRRO_AGENT_WORK_DIRECTORY     | Working directory for jobs           |

### Debugging

Debug mode can be enabled on the application by specifying the `--debug` flag on launch or setting log level to `DEBUG`.
