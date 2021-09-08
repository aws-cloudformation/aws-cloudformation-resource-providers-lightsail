package software.amazon.lightsail.instance.helpers.resource;

import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.AvailabilityZone;
import software.amazon.awssdk.services.lightsail.model.CreateInstancesRequest;
import software.amazon.awssdk.services.lightsail.model.DeleteInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceResponse;
import software.amazon.awssdk.services.lightsail.model.GetRegionsRequest;
import software.amazon.awssdk.services.lightsail.model.NotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.ResourceModel;

import static software.amazon.lightsail.instance.Translator.translateToSdkStartInstanceRequest;
import static software.amazon.lightsail.instance.Translator.translateToSdkStopInstanceRequest;

/**
 * Helper class to handle Instance resource related operations.
 */
@RequiredArgsConstructor
public class Instance implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    public AwsResponse update(AwsRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create Lightsail Instance based on the request.
     *
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse create(AwsRequest request) {
        logger.log(String.format("Creating Instance: %s", resourceModel.getInstanceName()));
        AwsResponse awsResponse = null;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((CreateInstancesRequest) request),
                proxyClient.client()::createInstances);
        logger.log(String.format("Successfully created Instance: %s", resourceModel.getInstanceName()));
        return awsResponse;
    }

    /**
     * Delete the Instance.
     *
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse delete(AwsRequest request) {
        logger.log(String.format("Deleting Instance: %s", resourceModel.getInstanceName()));
        AwsResponse awsResponse = null;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((DeleteInstanceRequest) request),
                proxyClient.client()::deleteInstance);
        logger.log(String.format("Successfully deleted Instance: %s", resourceModel.getInstanceName()));
        return awsResponse;
    }

    /**
     * Read the Instance.
     *
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse read(AwsRequest request) {
        logger.log(String.format("Reading Instance: %s", resourceModel.getInstanceName()));
        val awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                GetInstanceRequest.builder().instanceName(resourceModel.getInstanceName()).build(),
                proxyClient.client()::getInstance);
        logger.log(String.format("Instance: %s has successfully been read.", resourceModel.getInstanceName()));
        return awsResponse;
    }

    /**
     * Stop the Instance
     *
     * @param request
     *
     * @return AwsResponse
     */
    public AwsResponse stop(AwsRequest request) {
        logger.log(String.format("Stopping Instance: %s", resourceModel.getInstanceName()));
        AwsResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                translateToSdkStopInstanceRequest(resourceModel), proxyClient.client()::stopInstance);
        return awsResponse;
    }

    /**
     * Start the Instance
     *
     * @param request
     *
     * @return
     */
    public AwsResponse start(AwsRequest request) {
        logger.log(String.format("Starting Instance: %s", resourceModel.getInstanceName()));
        return proxyClient.injectCredentialsAndInvokeV2(translateToSdkStartInstanceRequest(resourceModel),
                proxyClient.client()::startInstance);
    }

    /**
     * Check if Instance has reached terminal state.
     *
     * @return
     */
    public boolean isStabilizedUpdate() {
        val awsResponse = ((GetInstanceResponse) this
                .read(GetInstanceRequest.builder().instanceName(resourceModel.getInstanceName()).build()));
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Instance: %s has stabilized. Current state: %s",
                resourceModel.getInstanceName(), currentState));
        return ("running".equalsIgnoreCase(currentState) || "stopped".equalsIgnoreCase(currentState));
    }

    /**
     * Check if Instance has reached terminal state along with AddOns.
     *
     * @return
     */
    public boolean isStabilizedCreate() {
        val awsResponse = ((GetInstanceResponse) this
                .read(GetInstanceRequest.builder().instanceName(resourceModel.getInstanceName()).build()));
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Instance: %s has stabilized. Current state: %s",
                resourceModel.getInstanceName(), currentState));
        val addOn = new AddOns(resourceModel, logger, proxyClient, resourceModelRequest);
        return addOn.isStabilizedCreate(awsResponse)
                && ("running".equalsIgnoreCase(currentState) || "stopped".equalsIgnoreCase(currentState));
    }

    /**
     * Check if the Instance has been successfully deleted.
     *
     * @return boolean
     */
    @Override
    public boolean isStabilizedDelete() {
        final boolean stabilized = false;
        logger.log(String.format("Checking if Instance: %s deletion has stabilized.",
                resourceModel.getInstanceName(), stabilized));
        try {
            this.read(GetInstanceRequest.builder().instanceName(resourceModel.getInstanceName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            logger.log(String.format("Instance: %s deletion has stabilized", resourceModel.getInstanceName()));
            return true;
        }
        return stabilized;
    }

    @Override
    public boolean isSafeExceptionCreateOrUpdate(Exception e) {
        return false;
    }

    @Override
    public boolean isSafeExceptionDelete(Exception e) {
        if (e instanceof CfnNotFoundException || e instanceof NotFoundException) {
            return true; // Its stabilized if the resource is gone..
        }
        return false;
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

    /**
     * Get the first sorted availability zone in the current region.
     *
     * @return String
     */
    public String getFirstAvailabilityZone() {
        return proxyClient
                .injectCredentialsAndInvokeV2(GetRegionsRequest.builder().includeAvailabilityZones(true).build(),
                        proxyClient.client()::getRegions)
                .regions().stream()
                .filter(region -> resourceModelRequest.getRegion().equalsIgnoreCase(region.name().toString()))
                .filter(region -> !region.availabilityZones().isEmpty()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Something wrong with fetching current region"))
                .availabilityZones().stream().map(AvailabilityZone::zoneName).sorted().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Something wrong with " + "fetching availability zone for current region"));
    }
}
