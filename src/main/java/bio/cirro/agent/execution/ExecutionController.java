package bio.cirro.agent.execution;

import bio.cirro.agent.models.AWSCredentials;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import lombok.AllArgsConstructor;

import java.util.List;

@Controller("/executions")
@AllArgsConstructor
public class ExecutionController {
    private final ExecutionService executionService;

    @Get
    public HttpResponse<List<ExecutionDto>> list() {
        return HttpResponse.ok(executionService.list());
    }

    @Post("/{executionId}/s3-token")
    public HttpResponse<AWSCredentials> generateS3Credentials(@PathVariable String executionId) {
        return HttpResponse.ok(executionService.generateS3Credentials(executionId));
    }

    @Put("/{executionId}/complete")
    public HttpResponse<Void> completeExecution(@PathVariable String executionId) {
        executionService.completeExecution(executionId);
        return HttpResponse.ok();
    }
}
