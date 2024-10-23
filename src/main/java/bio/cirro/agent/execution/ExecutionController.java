package bio.cirro.agent.execution;

import bio.cirro.agent.AgentTokenService;
import bio.cirro.agent.aws.AwsCredentials;
import bio.cirro.agent.models.UpdateStatusRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import lombok.AllArgsConstructor;

import java.util.List;

@Controller("/executions")
@AllArgsConstructor
public class ExecutionController {
    private final AgentTokenService agentTokenService;
    private final ExecutionService executionService;

    @Get
    public HttpResponse<List<ExecutionDto>> list() {
        return HttpResponse.ok(executionService.list());
    }

    @Post("/{executionId}/s3-token")
    public HttpResponse<AwsCredentials> generateS3Credentials(@PathVariable String executionId,
                                                              @Header("Authorization") String authorization) {
        agentTokenService.validate(authorization, executionId);
        return HttpResponse.ok(executionService.generateS3Credentials(executionId));
    }

    @Put("/{executionId}/status")
    public HttpResponse<Void> updateStatus(@PathVariable String executionId,
                                           @Body UpdateStatusRequest request,
                                           @Header("Authorization") String authorization) {
        if (request.status() == null) {
            return HttpResponse.badRequest();
        }
        agentTokenService.validate(authorization, executionId);
        executionService.updateStatus(executionId, request);
        return HttpResponse.ok();
    }
}
