package bio.cirro.agent.aws;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import software.amazon.awssdk.services.sts.StsClient;

@Factory
public class StsClientFactory {
    @Bean
    public StsClient stsClient() {
        return StsClient.create();
    }
}
