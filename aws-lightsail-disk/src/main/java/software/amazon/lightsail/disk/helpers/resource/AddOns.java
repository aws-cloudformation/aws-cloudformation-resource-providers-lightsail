package software.amazon.lightsail.disk.helpers.resource;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.AddOnType;
import software.amazon.awssdk.services.lightsail.model.GetDiskRequest;
import software.amazon.awssdk.services.lightsail.model.GetDiskResponse;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.disk.ResourceModel;

import static software.amazon.lightsail.disk.Translator.translateToDisableAddOnRequest;
import static software.amazon.lightsail.disk.Translator.translateToEnableAddOnRequest;

/**
 * Helper class to handle AddOns Interactions with the Disk resource.
 */
@RequiredArgsConstructor
public class AddOns implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    /**
     * Update Disk AddOn based on the desired resourceModel.
     *
     * @param request
     *
     * @return AwsResponse
     */
    public AwsResponse update(AwsRequest request) {
        AwsResponse awsResponse = null;
        val isEnableAddOn = isEnableAddOnRequest();
        if (isEnableAddOn) {
            logger.log(String.format("Trying to Enable AddOn for Disk: %s", resourceModel.getDiskName()));
            awsResponse = this.create(request);
        } else {
            logger.log(String.format("Trying to Disable AddOn for Disk: %s", resourceModel.getDiskName()));
            awsResponse = this.delete(request);
        }
        logger.log(String.format("AddOn has successfully been updated for Disk: %s", resourceModel.getDiskName()));
        return awsResponse;
    }

    /**
     * Enable AddOn on the Disk.
     *
     * @param request
     *
     * @return
     */
    @Override
    public AwsResponse create(AwsRequest request) {
        return proxyClient.injectCredentialsAndInvokeV2(translateToEnableAddOnRequest(resourceModel),
                proxyClient.client()::enableAddOn);
    }

    /**
     * Disable AddOn on the Disk.
     *
     * @param request
     *
     * @return
     */
    @Override
    public AwsResponse delete(AwsRequest request) {
        return proxyClient.injectCredentialsAndInvokeV2(translateToDisableAddOnRequest(resourceModel),
                proxyClient.client()::disableAddOn);
    }

    /**
     * Get Disk which will have the AddOns details
     *
     * @param request
     *
     * @return
     */
    @Override
    public AwsResponse read(AwsRequest request) {
        return new Disk(resourceModel, logger, proxyClient, resourceModelRequest).read(request);
    }

    /**
     * Check if AddOn has reached the terminal state.
     *
     * @return boolean
     */
    @Override
    public boolean isStabilizedUpdate() {
        val awsResponse = (GetDiskResponse) this
                .read(GetDiskRequest.builder().diskName(resourceModel.getDiskName()).build());
        val currentState = awsResponse.disk().addOns() == null || awsResponse.disk().addOns().size() == 0 ? "Pending"
                : awsResponse.disk().addOns().get(0).status();
        logger.log(String.format("Checking if AddOn has stabilized for Disk: %s. Current state %s",
                resourceModel.getDiskName(), currentState));
        return "enabled".equalsIgnoreCase(currentState) || "disabled".equalsIgnoreCase(currentState);
    }

    /**
     * Check if addOn passed during create is stabilized. It has parameter of GetDiskResponse, we do this to making
     * double call one to check Disk State and one to check AddOn State.
     *
     * @param awsResponse
     *
     * @return
     */
    public boolean isStabilizedCreate(final GetDiskResponse awsResponse) {
        // now we have only one AddOn, so checking 0th index directly
        if (resourceModel.getAddOns() != null && resourceModel.getAddOns().size() > 0
                && resourceModel.getAddOns().get(0) != null
                && (Strings.isNullOrEmpty(resourceModel.getAddOns().get(0).getStatus()) ||
                "enabled".equalsIgnoreCase(resourceModel.getAddOns().get(0).getStatus()))) {
            val currentState = awsResponse.disk().addOns() == null || awsResponse.disk().addOns().size() == 0
                    ? "Pending" : awsResponse.disk().addOns().get(0).status();
            logger.log(String.format("Checking if AddOn has stabilized for Disk: %s. Current state %s",
                    resourceModel.getDiskName(), currentState));

            // Enabling and Disabled are the terminal state, In stabilize all we do is wait for terminal state.
            return "enabled".equalsIgnoreCase(currentState);
        }

        // If there is no add-on in the request make it pass stabilize.
        return true;
    }

    @Override
    public boolean isStabilizedDelete() {
        return false;
    }

    /**
     * Check if the resource model request is the enable AddOn request or the disable AddOn request.
     *
     * Only AddOn we have is Auto Snapshot AddOn
     *
     * if AddOn status is not present then we will treat it as enabling AddOn If Status is enable or enabling then we
     * will treat it as enabling AddOn If AddOn itself is not present then it will be disable AddOn If AddOn status is
     * not enable or enabling then we will treat it as disabling AddOn
     *
     *
     * @return
     */
    private boolean isEnableAddOnRequest() {
        if (resourceModel.getAddOns() != null && resourceModel.getAddOns().size() > 0) {
            val autoSnapshotAddOn = resourceModel.getAddOns().stream()
                    .filter(addOn -> AddOnType.AUTO_SNAPSHOT.toString().equalsIgnoreCase(addOn.getAddOnType()))
                    .findFirst();

            // If AddOn status is not present -> Enabling
            // If Status is enable or enabling -> Enabling
            // If AddOn Not present -> Disable
            // If AddOn status not enable/enabling -> Disabling
            if (autoSnapshotAddOn.isPresent() && (autoSnapshotAddOn.get().getStatus() == null
                    || autoSnapshotAddOn.get().getStatus().toLowerCase().startsWith("enabled"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * For any Write operations of AddOns in case of Create or Update. Check if the thrown exception is safe exception
     * which we can skip and treat as success.
     *
     * @param e
     *
     * @return boolean
     */
    @Override
    public boolean isSafeExceptionCreateOrUpdate(Exception e) {
        if (e instanceof AwsServiceException) {
            val exp = (AwsServiceException) e;
            if (exp.awsErrorDetails().errorMessage().contains("The addOn is already in requested state")
                    || exp.awsErrorDetails().errorMessage().contains("AutoSnapshot not enabled for the resource")) {
                logger.log("This error is expected at this stage. Continuing execution.");
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSafeExceptionDelete(Exception e) {
        return false;
    }
}
