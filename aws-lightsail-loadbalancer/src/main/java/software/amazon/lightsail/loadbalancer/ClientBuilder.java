package software.amazon.lightsail.loadbalancer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
    return LightsailClient.builder().httpClient(LambdaWrapper.HTTP_CLIENT).build();
  }
}
