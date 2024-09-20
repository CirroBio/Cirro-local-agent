## Cirro Agent

Cirro Agent is a daemon process that allows you to submit jobs to local compute resources.


## Running

Run the agent using IntelliJ IDEA or run through the built jar file:

```
./gradlew shadowJar
java -jar build/libs/cirro-agent-0.1-all.jar --url=http://localhost:8080 --token=token
```

You can also build & run through the native executable:

```
./gradlew nativeImage
./build/native/nativeCompile/cirro-agent --url=http://localhost:8080 --token=token
```

Arguments:

| Argument               | Environment Variable       | Description                                     |
|------------------------|----------------------------|-------------------------------------------------|
| `--id`                 | `AGENT_ID`                 | ID of the agent                                 |
| `--url`                | `AGENT_SERVER_URL`         | URL of the server to connect to                 |
| `--token`              | `AGENT_TOKEN`              | Bearer token for authentication                 |
| `--heartbeat-interval` | `AGENT_HEARTBEAT_INTERVAL` | Interval in seconds to send heartbeat to server |
| `--debug`              | `AGENT_DEBUG`              | Enable debug logging                            |
