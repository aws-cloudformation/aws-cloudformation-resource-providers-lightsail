package software.amazon.lightsail.container.helpers.resource;

import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.container.ResourceModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static software.amazon.lightsail.container.Translator.translateFromReadResponse;

/**
 * Helper class to handle Tag Interactions with the Container resource.
 */
@RequiredArgsConstructor
public class Tags implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    /**
     * Update Tag of Container. Add Tags that are not there during create and remove tags that are removed in current
     * resource model.
     *
     * @param awsRequest
     *
     * @return
     */
    public AwsResponse update(AwsRequest awsRequest) {
        removeTags();
        return addTags();
    }

    /**
     * Get the tags that are there in the current and not there in desired tags. Remove tags not checking the values.
     * since Remove is by the key, will remove all values.
     *
     * @param currentTags
     * @param desiredTags
     *
     * @return
     */
    private static Collection<Tag> getTagsNeedToBeRemoved(Set<software.amazon.lightsail.container.Tag> currentTags,
            Set<software.amazon.lightsail.container.Tag> desiredTags) {
        return getTags(currentTags, desiredTags);
    }

    private static Collection<Tag> getTags(Set<software.amazon.lightsail.container.Tag> currentTags,
            Set<software.amazon.lightsail.container.Tag> desiredTags) {
        return currentTags.stream()
                .filter(tag -> tag.getKey() != null && desiredTags.stream()
                        .noneMatch(curTag -> curTag.getKey() != null && tag.getKey().equals(curTag.getKey())
                                && ((tag.getValue() == null && curTag.getValue() == null)
                                        || (tag.getValue() != null && tag.getValue().equals(curTag.getValue()))
                                        || (curTag.getValue() != null && curTag.getValue().equals(tag.getValue())))))
                .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build()).collect(Collectors.toList());
    }

    /**
     * Get the tags that are there in the desired and not there in current tags
     *
     * @param currentTags
     * @param desiredTags
     *
     * @return
     */
    private static Collection<Tag> getTagsNeedToBeAdded(Set<software.amazon.lightsail.container.Tag> currentTags,
            Set<software.amazon.lightsail.container.Tag> desiredTags) {
        return getTags(desiredTags, currentTags);
    }

    public AwsResponse addTags() {
        ResourceModel desiredResourceModel = resourceModelRequest.getDesiredResourceState();
        ResourceModel currentResourceModel = getCurrentResourceModelFromLightsail();
        final Set<software.amazon.lightsail.container.Tag> currentTags = currentResourceModel.getTags() != null
                ? currentResourceModel.getTags() : new HashSet<>();
        final Set<software.amazon.lightsail.container.Tag> desiredTags = desiredResourceModel.getTags() != null
                ? desiredResourceModel.getTags() : new HashSet<>();
        val tagsToAdd = getTagsNeedToBeAdded(currentTags, desiredTags);
        if (tagsToAdd.size() > 0) {
            val addTagRequest = TagResourceRequest.builder().resourceName(resourceModel.getServiceName()).tags(tagsToAdd)
                    .build();
            logger.log(String.format("Tagging Container: %s with Tags: %s", resourceModel.getServiceName(), tagsToAdd));
            return proxyClient.injectCredentialsAndInvokeV2(addTagRequest, proxyClient.client()::tagResource);
        }
        return TagResourceResponse.builder().build();
    }

    public AwsResponse removeTags() {
        ResourceModel desiredResourceModel = resourceModelRequest.getDesiredResourceState();
        ResourceModel currentResourceModel = getCurrentResourceModelFromLightsail();
        final Set<software.amazon.lightsail.container.Tag> currentTags = currentResourceModel.getTags() != null
                ? currentResourceModel.getTags() : new HashSet<>();
        final Set<software.amazon.lightsail.container.Tag> desiredTags = desiredResourceModel.getTags() != null
                ? desiredResourceModel.getTags() : new HashSet<>();
        val tagsToRemove = getTagsNeedToBeRemoved(currentTags, desiredTags);
        if (tagsToRemove.size() > 0) {
            val removeTagRequest = UntagResourceRequest.builder().resourceName(resourceModel.getServiceName())
                    .tagKeys(tagsToRemove.stream().map(Tag::key).collect(Collectors.toSet())).build();
            logger.log(String.format("Un-Tagging Tags: %s from Container: %s", tagsToRemove, resourceModel.getServiceName()));
            return proxyClient.injectCredentialsAndInvokeV2(removeTagRequest, proxyClient.client()::untagResource);
        }
        return UntagResourceResponse.builder().build();
    }

    private ResourceModel getCurrentResourceModelFromLightsail() {
        return translateFromReadResponse(new Container(resourceModel, logger, proxyClient, resourceModelRequest)
                .read(GetContainerServicesRequest.builder().serviceName(resourceModel.getServiceName()).build()));
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
