package software.amazon.lightsail.distribution.helpers.resource;

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
import software.amazon.lightsail.distribution.ResourceModel;

/**
 * Helper class to handle Distribution operations.
 */
@RequiredArgsConstructor
public class Distribution implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    public AwsResponse update(AwsRequest request) {
        AwsResponse awsResponse;
        logger.log(String.format("Updating Distribution: %s", resourceModel.getDistributionName()));
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((UpdateDistributionRequest) request),
                proxyClient.client()::updateDistribution);
        logger.log(String.format("Successfully updated Distribution: %s", resourceModel.getDistributionName()));
        return awsResponse;
    }

    public AwsResponse updateBundle(AwsRequest request) {
        AwsResponse awsResponse = UpdateDistributionBundleResponse.builder().build();
        if (!isUpdateBundleRequired()) {
            logger.log(String.format("Update not required for Distribution bundle: %s", resourceModel.getDistributionName()));
            return awsResponse;
        }
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((UpdateDistributionBundleRequest) request),
                proxyClient.client()::updateDistributionBundle);
        logger.log(String.format("Successfully updated Distribution bundle for: %s", resourceModel.getDistributionName()));
        return awsResponse;
    }

    public AwsResponse detachCertificate(AwsRequest request) {
        AwsResponse awsResponse = DetachCertificateFromDistributionResponse.builder().build();
        val distribution = ((GetDistributionsResponse) this.read(GetDistributionsRequest.builder()
                .distributionName(resourceModel.getDistributionName()).build())).distributions().get(0);
        if (distribution.certificateName() == null || (distribution.certificateName() != null &&
                distribution.certificateName().equals(resourceModel.getCertificateName()))) {
            logger.log(String.format("No detach certificate needed for Distribution: %s", resourceModel.getDistributionName()));
            return awsResponse;
        }

        logger.log(String.format("Detaching Certificate from Distribution: %s", resourceModel.getDistributionName()));
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((DetachCertificateFromDistributionRequest) request),
                proxyClient.client()::detachCertificateFromDistribution);
        logger.log(String.format("Successfully detached Certificate from Distribution: %s", resourceModel.getDistributionName()));
        return awsResponse;
    }

    public AwsResponse attachCertificate(AwsRequest request) {
        AwsResponse awsResponse = AttachCertificateToDistributionResponse.builder().build();
        val distribution = ((GetDistributionsResponse) this.read(GetDistributionsRequest.builder()
                .distributionName(resourceModel.getDistributionName()).build())).distributions().get(0);
        if (resourceModel.getCertificateName() == null || (resourceModel.getCertificateName()!= null &&
                resourceModel.getCertificateName().equals(distribution.certificateName()))) {
            logger.log(String.format("No attach certificate needed for Distribution: %s", resourceModel.getDistributionName()));
            return awsResponse;
        }

        logger.log(String.format("Attaching Certificate to Distribution: %s", resourceModel.getDistributionName()));
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((AttachCertificateToDistributionRequest) request),
                proxyClient.client()::attachCertificateToDistribution);
        logger.log(String.format("Successfully attached Certificate to Distribution: %s", resourceModel.getDistributionName()));
        return awsResponse;
    }

    @Override
    public AwsResponse create(AwsRequest request) {
        logger.log(String.format("Creating Distribution: %s", resourceModel.getDistributionName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((CreateDistributionRequest) request),
                proxyClient.client()::createDistribution);
        logger.log(String.format("Successfully created Distribution: %s", resourceModel.getDistributionName()));
        return awsResponse;
    }

    @Override
    public AwsResponse delete(AwsRequest request) {
        logger.log(String.format("Deleting Distribution: %s", resourceModel.getDistributionName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((DeleteDistributionRequest) request),
                proxyClient.client()::deleteDistribution);
        logger.log(String.format("Successfully deleted Distribution: %s", resourceModel.getDistributionName()));
        return awsResponse;
    }

    /**
     * Read Distribution.
     *
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse read(AwsRequest request) {
        val distributionName = ((GetDistributionsRequest) request).distributionName();
        logger.log(String.format("Reading Distribution: %s", distributionName));
        return proxyClient.injectCredentialsAndInvokeV2(GetDistributionsRequest.builder().distributionName(distributionName).build(),
                proxyClient.client()::getDistributions);
    }

    @Override
    public boolean isStabilizedUpdate() {
        val awsResponse = ((GetDistributionsResponse) this
                .read(GetDistributionsRequest.builder().distributionName(resourceModel.getDistributionName()).build()));
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Distribution: %s has stabilized. Current state: %s",
                resourceModel.getDistributionName(), currentState));
        return ("Deployed".equalsIgnoreCase(currentState));
    }

    @Override
    public boolean isStabilizedDelete() {
        final boolean stabilized = false;
        logger.log(String.format("Checking if Distribution: %s deletion has stabilized.",
                resourceModel.getDistributionName(), stabilized));
        try {
            this.read(GetDistributionsRequest.builder().distributionName(resourceModel.getDistributionName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            logger.log(String.format("Distribution: %s deletion has stabilized", resourceModel.getDistributionName()));
            return true;
        }
        return stabilized;
    }

    /**
     * Checking to see if update is required for the Distribution bundle.
     *
     * @return
     */
    public boolean isUpdateBundleRequired() {
        val distribution = ((GetDistributionsResponse) this.read(GetDistributionsRequest.builder()
                .distributionName(resourceModel.getDistributionName()).build())).distributions().get(0);
        if (resourceModel.getBundleId().equalsIgnoreCase(distribution.bundleId())) {
            return false;
        }
        return true;
    }

    /**
     * Get Current state of the Distribution.
     *
     * @return
     *
     * @param awsResponse
     */
    private String getCurrentState(GetDistributionsResponse awsResponse) {
        val distribution = awsResponse.distributions().get(0);
        return distribution.status() == null ? "Pending" : distribution.status();
    }

    @Override
    public boolean isSafeExceptionCreateOrUpdate(Exception e) {
        return false;
    }

    @Override
    public boolean isSafeExceptionDelete(Exception e) {
        if (e instanceof CfnNotFoundException || e instanceof NotFoundException || e instanceof InvalidInputException) {
            return true; // Its stabilized if the resource is gone..
        }
        return false;
    }
}
