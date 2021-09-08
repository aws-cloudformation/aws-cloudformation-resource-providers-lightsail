package software.amazon.lightsail.disk;

import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.model.AddOnRequest;
import software.amazon.awssdk.services.lightsail.model.AddOnType;
import software.amazon.awssdk.services.lightsail.model.CreateDiskRequest;
import software.amazon.awssdk.services.lightsail.model.DeleteDiskRequest;
import software.amazon.awssdk.services.lightsail.model.DetachDiskRequest;
import software.amazon.awssdk.services.lightsail.model.DisableAddOnRequest;
import software.amazon.awssdk.services.lightsail.model.Disk;
import software.amazon.awssdk.services.lightsail.model.EnableAddOnRequest;
import software.amazon.awssdk.services.lightsail.model.GetDiskRequest;
import software.amazon.awssdk.services.lightsail.model.GetDiskResponse;
import software.amazon.awssdk.services.lightsail.model.GetDisksRequest;
import software.amazon.awssdk.services.lightsail.model.GetDisksResponse;
import software.amazon.awssdk.services.lightsail.model.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is a centralized placeholder for - api request construction - object translation to/from aws sdk -
 * resource model construction for read/list handlers
 */

public class Translator {

    /**
     * Request to create a resource
     *
     * @param model
     *            resource model
     *
     * @return awsRequest the aws service request to create a resource
     */
    public static AwsRequest translateToCreateRequest(final ResourceModel model) {
        return CreateDiskRequest.builder().diskName(model.getDiskName()).addOns(translateAddOnsToSdk(model.getAddOns()))
                .availabilityZone(model.getAvailabilityZone()).sizeInGb(model.getSizeInGb())
                .tags(translateTagsToSdk(model.getTags())).build();
    }

    /**
     * Request to read a resource
     *
     * @param model
     *            resource model
     *
     * @return awsRequest the aws service request to describe a resource
     */
    public static AwsRequest translateToReadRequest(final ResourceModel model) {
        return GetDiskRequest.builder().diskName(model.getDiskName()).build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param awsResponse
     *            the aws service describe resource response
     *
     * @return model resource model
     */
    public static ResourceModel translateFromReadResponse(final AwsResponse awsResponse) {
        val getDiskResponse = (GetDiskResponse) awsResponse;
        if (getDiskResponse == null) {
            return ResourceModel.builder().build();
        }
        val disk = getDiskResponse.disk();
        return translateSDKDiskToResourceModel(disk);
    }

    private static ResourceModel translateSDKDiskToResourceModel(final Disk disk) {
        return ResourceModel.builder().addOns(translateSDKtoAddOns(disk.addOns()))
                .availabilityZone(disk.location() == null ? null : disk.location().availabilityZone())
                .resourceType(disk.resourceType() == null ? null : disk.resourceType().name())
                .attachedTo(disk.attachedTo()).attachmentState(disk.attachmentState()).diskName(disk.name())
                .tags(translateSDKtoTag(disk.tags())).location(translateSDKtoLocation(disk.location()))
                .sizeInGb(disk.sizeInGb()).state(disk.state() == null ? null : disk.state().name()).iops(disk.iops())
                .diskArn(disk.arn()).isAttached(disk.isAttached()).supportCode(disk.supportCode()).path(disk.path())
                .build();
    }

    /**
     * Request to delete a resource
     *
     * @param model
     *            resource model
     *
     * @return awsRequest the aws service request to delete a resource
     */
    public static AwsRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteDiskRequest.builder().diskName(model.getDiskName()).forceDeleteAddOns(true)
                // always set to True for Cloudformation users, since delete happens without manual template
                .build();
    }

    /**
     * Request to list resources
     *
     * @param nextToken
     *            token passed to the aws service list resources request
     *
     * @return awsRequest the aws service request to list resources within aws account
     */
    static AwsRequest translateToListRequest(final String nextToken) {
        return GetDisksRequest.builder().pageToken(nextToken).build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     *
     * @param awsResponse
     *            the aws service describe resource response
     *
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
        val getDisksResponse = (GetDisksResponse) awsResponse;
        return getDisksResponse.disks().stream().map(Translator::translateSDKDiskToResourceModel)
                .collect(Collectors.toList());
    }

    private static Collection<AddOnRequest> translateAddOnsToSdk(List<AddOn> addOns) {
        if (addOns == null) {
            return null;
        }
        // Get only Enabled status AddOn. Only that we need to enable now.
        // If the AddOn status is not provided then its for enabling and if status starts with enable then it will be
        // enabled, enabling, enable so we need only them
        return addOns.stream()
                .filter(addon -> addon.getStatus() == null || addon.getStatus().toLowerCase().startsWith("enable"))
                .map(addOn -> AddOnRequest.builder().addOnType(addOn.getAddOnType())
                        .autoSnapshotAddOnRequest(
                                translateAutoSnapshotAddOnRequest(addOn.getAutoSnapshotAddOnRequest()))
                        .build())
                .collect(Collectors.toSet());
    }

    private static software.amazon.awssdk.services.lightsail.model.AutoSnapshotAddOnRequest translateAutoSnapshotAddOnRequest(
            AutoSnapshotAddOn autoSnapshotAddOnRequest) {
        return autoSnapshotAddOnRequest != null
                ? software.amazon.awssdk.services.lightsail.model.AutoSnapshotAddOnRequest.builder()
                        .snapshotTimeOfDay(autoSnapshotAddOnRequest.getSnapshotTimeOfDay()).build()
                : null;
    }

    private static List<AddOn> translateSDKtoAddOns(
            List<software.amazon.awssdk.services.lightsail.model.AddOn> addOns) {
        return addOns == null ? null
                : addOns.stream().map(addOn -> AddOn.builder().addOnType(addOn.name())
                        .autoSnapshotAddOnRequest(
                                AutoSnapshotAddOn.builder().snapshotTimeOfDay(addOn.snapshotTimeOfDay()).build())
                        .status(addOn.status()).build()).collect(Collectors.toList());
    }

    private static Set<software.amazon.awssdk.services.lightsail.model.Tag> translateTagsToSdk(Collection<Tag> tags) {
        return tags == null ? null : tags.stream().map(tag -> software.amazon.awssdk.services.lightsail.model.Tag
                .builder().key(tag.getKey()).value(tag.getValue()).build()).collect(Collectors.toSet());
    }

    private static Set<Tag> translateSDKtoTag(List<software.amazon.awssdk.services.lightsail.model.Tag> tags) {
        return tags == null ? null : tags.stream().map(tag -> Tag.builder().key(tag.key()).value(tag.value()).build())
                .collect(Collectors.toSet());
    }

    private static Location translateSDKtoLocation(ResourceLocation location) {
        return location == null ? null : Location.builder().availabilityZone(location.availabilityZone())
                .regionName(location.regionNameAsString()).build();
    }

    public static EnableAddOnRequest translateToEnableAddOnRequest(ResourceModel resourceModel) {
        Optional<AddOn> autoSnapshotAddOn = Optional.empty();
        if (resourceModel.getAddOns() != null && resourceModel.getAddOns().size() > 0) {
            autoSnapshotAddOn = resourceModel.getAddOns().stream()
                    .filter(addOn -> AddOnType.AUTO_SNAPSHOT.toString().equalsIgnoreCase(addOn.getAddOnType()))
                    .findFirst();
        }
        return EnableAddOnRequest.builder().resourceName(resourceModel.getDiskName())
                .addOnRequest(AddOnRequest.builder().addOnType(AddOnType.AUTO_SNAPSHOT.toString())
                        .autoSnapshotAddOnRequest(translateAutoSnapshotAddOnRequest(
                                autoSnapshotAddOn.map(AddOn::getAutoSnapshotAddOnRequest).orElse(null)))
                        .build())
                .build();
    }

    public static DisableAddOnRequest translateToDisableAddOnRequest(ResourceModel resourceModel) {
        return DisableAddOnRequest.builder().resourceName(resourceModel.getDiskName())
                .addOnType(AddOnType.AUTO_SNAPSHOT.toString()).build();
    }

    static DetachDiskRequest translateToDetachDiskRequest(final ResourceModel resourceModel) {
        return DetachDiskRequest.builder().diskName(resourceModel.getDiskName()).build();
    }

}
