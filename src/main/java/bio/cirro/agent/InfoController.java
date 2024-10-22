package bio.cirro.agent;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Controller("/info")
@AllArgsConstructor
public class InfoController {
    private final EmbeddedServer embeddedServer;
    private final AgentConfig agentConfig;

    @Get
    public HttpResponse<InfoResponse> info() {
        return HttpResponse.ok(
                InfoResponse.builder()
                        .agentEndpoint(embeddedServer.getURI().toString())
                        .agentVersion(agentConfig.getVersion())
                        .build()
        );
    }

    @Builder
    @Serdeable
    public record InfoResponse(
            String agentEndpoint,
            String agentVersion
    ) {}
}
