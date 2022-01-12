package software.amazon.lightsail.distribution;

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
            // Having retry in the client might result in unnecessary extra calls to the Service.
            .overrideConfiguration(ClientOverrideConfiguration.builder().retryPolicy(RetryPolicy.none()).build())
            .build();
  }
}
