package software.amazon.lightsail.staticip.helpers.resource;

import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.staticip.ResourceModel;

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
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse read(AwsRequest request) {
        logger.log(String.format("Reading Instance: %s", resourceModel.getAttachedTo()));
        val awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                GetInstanceRequest.builder().instanceName(resourceModel.getAttachedTo()).build(),
                proxyClient.client()::getInstance);
        logger.log(String.format("Instance: %s has successfully been read.", resourceModel.getAttachedTo()));
        return awsResponse;
    }

    /**
     * Check if Instance has reached running state.
     *
     * @return
     */
    public boolean isStabilized() {
        if (resourceModelRequest.getDesiredResourceState().getAttachedTo() == null) {
            return true;
        }
        val awsResponse = ((GetInstanceResponse) this.read(GetInstanceRequest.builder()
                .instanceName(resourceModelRequest.getDesiredResourceState().getAttachedTo()).build()));
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Instance: %s has stabilized. Current state: %s",
                resourceModel.getAttachedTo(), currentState));
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
