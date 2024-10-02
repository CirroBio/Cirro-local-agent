package bio.cirro.agent;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/agent")
public class AgentController {
    @Get
    public String hello() {
        return "Hello World!";
    }
}
