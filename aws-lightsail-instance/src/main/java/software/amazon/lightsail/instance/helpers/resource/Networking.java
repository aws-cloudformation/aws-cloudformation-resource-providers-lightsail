package software.amazon.lightsail.instance.helpers.resource;

import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.PutInstancePublicPortsRequest;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.ResourceModel;

/**
 * Helper class to handle Networking Interactions with the Instance resource.
 */
@RequiredArgsConstructor
public class Networking implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    /**
     * Update Instance port. Instance port will be update only if the ports are provided. To remove the port access that
     * is currently public then the port has to be provided with the private. We do this to prevent default ports access
     * to go away accidentall in case of update.
     *
     * @param awsRequest
     *
     * @return
     */
    public AwsResponse update(AwsRequest awsRequest) {
        val instance = new Instance(resourceModel, logger, proxyClient, resourceModelRequest);
        AwsResponse awsResponse = null;
        if (needPortUpdate()) {
            logger.log(String.format("Updating Ports for Instance: %s", resourceModel.getInstanceName()));
            awsResponse = proxyClient.injectCredentialsAndInvokeV2((PutInstancePublicPortsRequest) awsRequest,
                    proxyClient.client()::putInstancePublicPorts);
            logger.log(String.format("Ports have been successfully updated for Instance: %s.", resourceModel.getInstanceName()));
        } else {
            logger.log(String.format("No port update needed for Instance: %s", resourceModel.getInstanceName()));
        }
        return awsResponse;
    }

    /**
     * Check if Port need to be updated in the create request.
     *
     * @return
     */
    private boolean needPortUpdate() {
        return resourceModel.getNetworking() != null && resourceModel.getNetworking().getPorts() != null
                && resourceModel.getNetworking().getPorts().size() > 0;
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
