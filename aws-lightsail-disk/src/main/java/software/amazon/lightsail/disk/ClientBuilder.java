package software.amazon.lightsail.disk;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.LambdaWrapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientBuilder {

    /**
     * Build Lightsail Client
     *
     * @return LightsailClient
     */
    public static LightsailClient getClient() {
        return LightsailClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                // CFN registry has own retry logic on the handler failures.
                // Having retry in the client also making too many calls to the Service in case of
                // InsufficientInstanceCapacityError
                .overrideConfiguration(ClientOverrideConfiguration.builder().retryPolicy(RetryPolicy.none()).build())
                .build();
    }
}
