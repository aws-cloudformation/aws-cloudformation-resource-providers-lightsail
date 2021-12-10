package software.amazon.lightsail.bucket.helpers.resource;

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
import software.amazon.lightsail.bucket.ResourceModel;

import java.util.HashSet;
import java.util.Set;

import static software.amazon.lightsail.bucket.Translator.translateFromReadResponse;

/**
 * Helper class to handle Bucket operations.
 */
@RequiredArgsConstructor
public class Bucket implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    public AwsResponse update(AwsRequest request) {
        AwsResponse awsResponse = null;
        logger.log(String.format("Updating Bucket: %s", resourceModel.getBucketName()));
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((UpdateBucketRequest) request),
                proxyClient.client()::updateBucket);
        logger.log(String.format("Successfully updated Bucket: %s", resourceModel.getBucketName()));
        return awsResponse;
    }

    public AwsResponse updateBundle(AwsRequest request) {
        AwsResponse awsResponse = null;
        if (!isUpdateBundleRequired()) {
            logger.log(String.format("Update not required for Bucket bundle: %s", resourceModel.getBucketName()));
            return awsResponse;
        }
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((UpdateBucketBundleRequest) request),
                proxyClient.client()::updateBucketBundle);
        logger.log(String.format("Successfully updated Bucket bundle for: %s", resourceModel.getBucketName()));
        return awsResponse;
    }

    public AwsResponse setResourceAccess(String resource, Boolean isAdd) {
        String access = isAdd ? "allow" : "deny";
        AwsResponse awsResponse = null;
        val setResourceAccessRequest = SetResourceAccessForBucketRequest.builder()
                .bucketName(resourceModel.getBucketName()).resourceName(resource).access(access).build();
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(setResourceAccessRequest,
                proxyClient.client()::setResourceAccessForBucket);
        logger.log(String.format("Setting resource: %s access for Bucket: %s with access: %s", resource,
                resourceModel.getBucketName(), access));
        return awsResponse;
    }

    public AwsResponse detachInstances(AwsRequest request) {
        AwsResponse awsResponse = null;
        Set<String> desiredResources = resourceModelRequest.getDesiredResourceState().getResourcesReceivingAccess();
        Set<String> currentResources = getCurrentResourceModelFromLightsail().getResourcesReceivingAccess();

        Set<String> resourcesToRemove = setDifference(currentResources, desiredResources);
        logger.log("Resources to detach: " + resourcesToRemove.toString());
        for (val resource: resourcesToRemove) {
            setResourceAccess(resource, false);
        }

        return awsResponse;
    }

    public AwsResponse attachInstances(AwsRequest request) {
        AwsResponse awsResponse = null;
        Set<String> desiredResources = resourceModelRequest.getDesiredResourceState().getResourcesReceivingAccess();
        Set<String> currentResources = getCurrentResourceModelFromLightsail().getResourcesReceivingAccess();

        Set<String> resourcesToAdd = setDifference(desiredResources, currentResources);
        logger.log("Resources to attach: " + resourcesToAdd.toString());
        for (val resource: resourcesToAdd) {
            setResourceAccess(resource, true);
        }

        return awsResponse;
    }

    @Override
    public AwsResponse create(AwsRequest request) {
        logger.log(String.format("Creating Bucket: %s", resourceModel.getBucketName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((CreateBucketRequest) request),
                proxyClient.client()::createBucket);
        logger.log(String.format("Successfully created Bucket: %s", resourceModel.getBucketName()));
        return awsResponse;
    }

    @Override
    public AwsResponse delete(AwsRequest request) {
        logger.log(String.format("Deleting Bucket: %s", resourceModel.getBucketName()));
        AwsResponse awsResponse = null;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((DeleteBucketRequest) request),
                proxyClient.client()::deleteBucket);
        logger.log(String.format("Successfully deleted Bucket: %s", resourceModel.getBucketName()));
        return awsResponse;
    }

    /**
     * Read Bucket.
     *
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse read(AwsRequest request) {
        val bucketName = ((GetBucketsRequest) request).bucketName();
        logger.log(String.format("Reading Bucket: %s", bucketName));
        return proxyClient.injectCredentialsAndInvokeV2(GetBucketsRequest.builder().bucketName(bucketName)
                        .includeConnectedResources(true).build(), proxyClient.client()::getBuckets);
    }

    /**
     * Read all buckets.
     *
     * @param request
     *
     * @return AwsResponse
     */
    public AwsResponse readAll(AwsRequest request) {
        logger.log("Reading all buckets");
        return proxyClient.injectCredentialsAndInvokeV2(GetBucketsRequest.builder()
                .includeConnectedResources(true).build(), proxyClient.client()::getBuckets);
    }

    @Override
    public boolean isStabilizedUpdate() {
        val awsResponse = ((GetBucketsResponse) this
                .read(GetBucketsRequest.builder().bucketName(resourceModel.getBucketName()).build()));
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Bucket: %s has stabilized. Current state: %s",
                resourceModel.getBucketName(), currentState));
        return ("OK".equalsIgnoreCase(currentState));
    }

    public boolean isStabilizedCreate() {
        val awsResponse = ((GetBucketsResponse) this
                .read(GetBucketsRequest.builder().bucketName(resourceModel.getBucketName()).build()));
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Bucket: %s has stabilized. Current state: %s",
                resourceModel.getBucketName(), currentState));
        return ("OK".equalsIgnoreCase(currentState));
    }

    @Override
    public boolean isStabilizedDelete() {
        final boolean stabilized = false;
        logger.log(String.format("Checking if Bucket: %s deletion has stabilized.",
                resourceModel.getBucketName(), stabilized));
        try {
            this.read(GetBucketsRequest.builder().bucketName(resourceModel.getBucketName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            logger.log(String.format("Bucket: %s deletion has stabilized", resourceModel.getBucketName()));
            return true;
        }
        return stabilized;
    }

    /**
     * Checking to see if update is required for the bucket bundle.
     *
     * @return
     */
    public boolean isUpdateBundleRequired() {
        val bucket = ((GetBucketsResponse) this.read(GetBucketsRequest.builder()
                .bucketName(resourceModel.getBucketName()).build())).buckets().get(0);
        if (resourceModel.getBundleId().equalsIgnoreCase(bucket.bundleId())) {
            return false;
        }
        return true;
    }

    public ResourceModel getCurrentResourceModelFromLightsail() {
        return translateFromReadResponse(this.read(GetBucketsRequest.builder()
                .bucketName(resourceModel.getBucketName()).build()));
    }

    public Set<String> setDifference(Set<String> setOne, Set<String> setTwo) {
        if (setOne == null || setOne.size() == 0) {
            return new HashSet<>();
        }
        if (setTwo == null || setTwo.size() == 0) {
            return setOne == null ? new HashSet<>() : setOne;
        }
        Set<String> result = new HashSet<String>(setOne);
        result.removeIf(setTwo::contains);
        return result;
    }

    /**
     * Get Current state of the Bucket.
     *
     * @return
     *
     * @param awsResponse
     */
    private String getCurrentState(GetBucketsResponse awsResponse) {
        val bucket = awsResponse.buckets().get(0);
        return bucket.state() == null ? "Pending" : bucket.state().code();
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

}
