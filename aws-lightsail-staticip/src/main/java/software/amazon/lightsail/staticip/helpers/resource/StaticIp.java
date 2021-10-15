package software.amazon.lightsail.staticip.helpers.resource;

import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.staticip.ResourceModel;

import static software.amazon.lightsail.staticip.Translator.translateFromReadResponse;

/**
 * Helper class to handle StaticIp operations.
 */
@RequiredArgsConstructor
public class StaticIp implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    public AwsResponse update(AwsRequest request) {
        detach();
        return attach();
    }

    @Override
    public AwsResponse create(AwsRequest request) {
        logger.log(String.format("Allocating StaticIp: %s", resourceModel.getStaticIpName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((AllocateStaticIpRequest) request),
                proxyClient.client()::allocateStaticIp);
        logger.log(String.format("Successfully allocated StaticIp: %s", resourceModel.getStaticIpName()));
        return awsResponse;
    }

    @Override
    public AwsResponse delete(AwsRequest request) {
        logger.log(String.format("Releasing StaticIp: %s", resourceModel.getStaticIpName()));
        AwsResponse awsResponse = null;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((ReleaseStaticIpRequest) request),
                proxyClient.client()::releaseStaticIp);
        logger.log(String.format("Successfully released StaticIp: %s", resourceModel.getStaticIpName()));
        return awsResponse;
    }

    /**
     * Read StaticIp.
     *
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse read(AwsRequest request) {
        val staticIpName = ((GetStaticIpRequest) request).staticIpName();
        logger.log(String.format("Reading StaticIp: %s", staticIpName));
        return proxyClient.injectCredentialsAndInvokeV2(GetStaticIpRequest.builder().staticIpName(staticIpName).build(),
                proxyClient.client()::getStaticIp);
    }

    public boolean isStabilizedCreate() {
        logger.log(String.format("Checking if StaticIp: %s creation has been stabilized.",
                resourceModel.getStaticIpName()));
        try {
            this.read(GetStaticIpRequest.builder().staticIpName(resourceModel.getStaticIpName()).build());
        } catch (final Exception e) {
            if (e instanceof CfnNotFoundException || e instanceof NotFoundException) {
                return false;
            } else {
                throw e;
            }
        }
        logger.log(String.format("StaticIp: %s creation has stabilized", resourceModel.getStaticIpName()));
        return true;
    }

    @Override
    public boolean isStabilizedDelete() {
        final boolean stabilized = false;
        logger.log(String.format("Checking if StaticIp: %s release has stabilized.",
                resourceModel.getStaticIpName(), stabilized));
        try {
            this.read(GetStaticIpRequest.builder().staticIpName(resourceModel.getStaticIpName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            logger.log(String.format("StaticIp: %s release has stabilized", resourceModel.getStaticIpName()));
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

    @Override
    public boolean isStabilizedUpdate() {
        throw new UnsupportedOperationException();
    }

    private ResourceModel getCurrentResourceModelFromLightsail() {
        return translateFromReadResponse(this.read(GetStaticIpRequest.builder()
                .staticIpName(resourceModel.getStaticIpName()).build()));
    }

    public AwsResponse detach() {
        ResourceModel desiredResourceModel = resourceModelRequest.getDesiredResourceState();
        ResourceModel currentResourceModel = getCurrentResourceModelFromLightsail();

        if (desiredResourceModel.getAttachedTo() == null && currentResourceModel.getIsAttached()) {
            logger.log(String.format("Detaching StaticIp: %s from Instance: %s",
                    currentResourceModel.getStaticIpName(), currentResourceModel.getAttachedTo()));
            val detachStaticIpRequest = DetachStaticIpRequest.builder()
                    .staticIpName(desiredResourceModel.getStaticIpName()).build();
            return proxyClient.injectCredentialsAndInvokeV2(detachStaticIpRequest,
                    proxyClient.client()::detachStaticIp);
        }
        logger.log(String.format("No detach required for StaticIp: %s", currentResourceModel.getStaticIpName()));
        return null;
    }

    public AwsResponse attach() {
        ResourceModel desiredResourceModel = resourceModelRequest.getDesiredResourceState();
        ResourceModel currentResourceModel = getCurrentResourceModelFromLightsail();

        if (desiredResourceModel.getAttachedTo() != null) {
            if (currentResourceModel.getAttachedTo() != null &&
                    currentResourceModel.getAttachedTo().equals(desiredResourceModel.getAttachedTo())) {
                return null;
            }
            logger.log(String.format("Attaching StaticIp: %s to Instance: %s",
                    desiredResourceModel.getStaticIpName(), desiredResourceModel.getAttachedTo()));
            val attachStaticIpRequest = AttachStaticIpRequest.builder()
                    .staticIpName(desiredResourceModel.getStaticIpName()).instanceName(desiredResourceModel.getAttachedTo())
                    .build();
            return proxyClient.injectCredentialsAndInvokeV2(attachStaticIpRequest,
                    proxyClient.client()::attachStaticIp);
        }
        logger.log(String.format("No attach required for StaticIp: %s", currentResourceModel.getStaticIpName()));
        return null;
    }
}
