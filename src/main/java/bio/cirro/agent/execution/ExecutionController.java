package bio.cirro.agent.execution;

import bio.cirro.agent.models.AWSCredentials;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import lombok.AllArgsConstructor;

@Controller("/executions/{executionId}")
@AllArgsConstructor
public class ExecutionController {
    private final ExecutionService executionSessionService;

    @Post("/s3-token")
    public HttpResponse<AWSCredentials> generateExecutionS3Credentials(@PathVariable String executionId) {
        return HttpResponse.ok(executionSessionService.generateExecutionS3Credentials(executionId));
    }

    @Put("/complete")
    public HttpResponse<Void> completeExecution(@PathVariable String executionId) {
        executionSessionService.completeExecution(executionId);
        return HttpResponse.ok();
    }
}
