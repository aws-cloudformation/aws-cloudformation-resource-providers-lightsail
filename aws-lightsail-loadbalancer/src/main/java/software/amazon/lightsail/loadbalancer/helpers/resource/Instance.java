package software.amazon.lightsail.loadbalancer.helpers.resource;

import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceResponse;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.loadbalancer.ResourceModel;

/**
 * Helper class to handle Instance resource related operations.
 */
@RequiredArgsConstructor
public class Instance implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    /**
     * Read the Instance.
     *
     * @param instanceName
     *
     * @return AwsResponse
     */
    public AwsResponse read(String instanceName) {
        logger.log(String.format("Reading Instance: %s", instanceName));
        val awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                GetInstanceRequest.builder().instanceName(instanceName).build(),
                proxyClient.client()::getInstance);
        logger.log(String.format("Instance: %s has successfully been read.", instanceName));
        return awsResponse;
    }

    /**
     * Check if Instance has reached running state.
     *
     * @return
     */
    public boolean isStabilized(String instanceName) {
        val awsResponse = ((GetInstanceResponse) this.read(instanceName));
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Instance: %s has stabilized. Current state: %s",
                instanceName, currentState));
        return "running".equalsIgnoreCase(currentState);
    }


    /**
     * Get Current state of the Instance.
     *
     * @return
     *
     * @param awsResponse
     */
    private String getCurrentState(GetInstanceResponse awsResponse) {
        val instance = awsResponse.instance();
        return instance.state() == null ? "Pending" : instance.state().name();
    }

    @Override
    public AwsResponse create(AwsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AwsResponse delete(AwsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AwsResponse update(AwsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AwsResponse read(AwsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStabilizedUpdate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStabilizedDelete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSafeExceptionCreateOrUpdate(Exception e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSafeExceptionDelete(Exception e) {
        throw new UnsupportedOperationException();
    }
}
