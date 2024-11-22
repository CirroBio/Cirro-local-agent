package bio.cirro.agent;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;

@Controller()
@AllArgsConstructor
public class InfoController {

    @Get
    public HttpResponse<InfoResponse> info() {
        return HttpResponse.ok(new InfoResponse("OK"));
    }

    @Serdeable
    public record InfoResponse(String message) {
    }
}
