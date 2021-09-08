package software.amazon.lightsail.disk.helpers.resource;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.AvailabilityZone;
import software.amazon.awssdk.services.lightsail.model.CreateDiskRequest;
import software.amazon.awssdk.services.lightsail.model.DeleteDiskRequest;
import software.amazon.awssdk.services.lightsail.model.GetDiskRequest;
import software.amazon.awssdk.services.lightsail.model.GetDiskResponse;
import software.amazon.awssdk.services.lightsail.model.GetRegionsRequest;
import software.amazon.awssdk.services.lightsail.model.NotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.disk.ResourceModel;

/**
 * Helper class to handle Disk operations.
 */
@RequiredArgsConstructor
public class Disk implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    public AwsResponse update(AwsRequest request) {
        return null;
    }

    @Override
    public AwsResponse create(AwsRequest request) {
        logger.log(String.format("Creating Disk: %s", resourceModel.getDiskName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((CreateDiskRequest) request),
                proxyClient.client()::createDisk);
        logger.log(String.format("Successfully created Disk: %s", resourceModel.getDiskName()));
        return awsResponse;
    }

    @Override
    public AwsResponse delete(AwsRequest request) {
        logger.log(String.format("Deleting Disk: %s", resourceModel.getDiskName()));
        AwsResponse awsResponse = null;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((DeleteDiskRequest) request),
                proxyClient.client()::deleteDisk);
        logger.log(String.format("Successfully deleted Disk: %s", resourceModel.getDiskName()));
        return awsResponse;
    }

    /**
     * Read Disk to check its state and attachment state.
     *
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse read(AwsRequest request) {
        val diskName = ((GetDiskRequest) request).diskName();
        logger.log(String.format("Reading Disk: %s", diskName));
        return proxyClient.injectCredentialsAndInvokeV2(GetDiskRequest.builder().diskName(diskName).build(),
                proxyClient.client()::getDisk);
    }

    @Override
    public boolean isStabilizedUpdate() {
        val awsResponse = ((GetDiskResponse) this
                .read(GetDiskRequest.builder().diskName(resourceModel.getDiskName()).build()));
        val addOn = new AddOns(resourceModel, logger, proxyClient, resourceModelRequest);
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Disk: %s has stabilized. Current state: %s",
                resourceModel.getDiskName(), currentState));
        return ("available".equalsIgnoreCase(currentState) || "in-use".equalsIgnoreCase(currentState));
    }

    public boolean isStabilizedCreate() {
        val awsResponse = ((GetDiskResponse) this
                .read(GetDiskRequest.builder().diskName(resourceModel.getDiskName()).build()));
        val addOn = new AddOns(resourceModel, logger, proxyClient, resourceModelRequest);
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Disk: %s has stabilized. Current state: %s",
                resourceModel.getDiskName(), currentState));
        return addOn.isStabilizedCreate(awsResponse)
                && ("available".equalsIgnoreCase(currentState) || "in-use".equalsIgnoreCase(currentState));
    }

    @Override
    public boolean isStabilizedDelete() {
        final boolean stabilized = false;
        logger.log(String.format("Checking if Disk: %s deletion has stabilized.",
                resourceModel.getDiskName(), stabilized));
        try {
            this.read(GetDiskRequest.builder().diskName(resourceModel.getDiskName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            logger.log(String.format("Disk: %s deletion has stabilized", resourceModel.getDiskName()));
            return true;
        }
        return stabilized;
    }

    /**
     * Get Current state of the Disk.
     *
     * @return
     *
     * @param awsResponse
     */
    private String getCurrentState(GetDiskResponse awsResponse) {
        val disk = awsResponse.disk();
        return disk.state() == null ? "Pending" : disk.state().name();
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
     * Check if current disk is free and not used by any Instance.
     *
     * @return boolean
     */
    public boolean isDiskFree() {
        val disk = ((GetDiskResponse) this.read(GetDiskRequest.builder().diskName(resourceModel.getDiskName()).build()))
                .disk();
        logger.log(String.format("Waiting for Disk: %s Ready to be ready", disk.name()));
        return disk != null && (disk.isAttached() == null || !disk.isAttached())
                && Strings.isNullOrEmpty(disk.attachedTo()) && "detached".equalsIgnoreCase(disk.attachmentState())
                && !"pending".equalsIgnoreCase(disk.state() == null ? "pending" : disk.state().toString());
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
