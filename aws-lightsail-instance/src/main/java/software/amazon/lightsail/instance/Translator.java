package software.amazon.lightsail.instance;

import com.google.common.collect.ImmutableSet;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.model.AddOnRequest;
import software.amazon.awssdk.services.lightsail.model.AddOnType;
import software.amazon.awssdk.services.lightsail.model.AttachDiskRequest;
import software.amazon.awssdk.services.lightsail.model.CreateInstancesRequest;
import software.amazon.awssdk.services.lightsail.model.DeleteInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.DetachDiskRequest;
import software.amazon.awssdk.services.lightsail.model.DisableAddOnRequest;
import software.amazon.awssdk.services.lightsail.model.EnableAddOnRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceResponse;
import software.amazon.awssdk.services.lightsail.model.GetInstancesRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstancesResponse;
import software.amazon.awssdk.services.lightsail.model.Instance;
import software.amazon.awssdk.services.lightsail.model.InstanceHardware;
import software.amazon.awssdk.services.lightsail.model.InstanceNetworking;
import software.amazon.awssdk.services.lightsail.model.InstancePortInfo;
import software.amazon.awssdk.services.lightsail.model.InstanceState;
import software.amazon.awssdk.services.lightsail.model.PortInfo;
import software.amazon.awssdk.services.lightsail.model.PutInstancePublicPortsRequest;
import software.amazon.awssdk.services.lightsail.model.ResourceLocation;
import software.amazon.awssdk.services.lightsail.model.StartInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.StopInstanceRequest;

import java.util.ArrayList;
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
        return CreateInstancesRequest.builder().instanceNames(model.getInstanceName()).userData(model.getUserData())
                .tags(translateTagsToSdk(model.getTags())).blueprintId(model.getBlueprintId())
                .bundleId(model.getBundleId()).availabilityZone(model.getAvailabilityZone())
                .keyPairName(model.getKeyPairName()).addOns(translateAddOnsToSdk(model.getAddOns())).build();
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
        return GetInstanceRequest.builder().instanceName(model.getInstanceName()).build();
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
        val getInstanceResponse = (GetInstanceResponse) awsResponse;
        if (getInstanceResponse == null) {
            return ResourceModel.builder().build();
        }
        val instance = getInstanceResponse.instance();
        return translateSDKInstanceToResourceModel(instance);
    }

    private static ResourceModel translateSDKInstanceToResourceModel(final Instance instance) {
        return ResourceModel.builder()
                .availabilityZone(instance.location() == null ? null : instance.location().availabilityZone())
                .addOns(translateSDKtoAddOns(instance.addOns())).blueprintId(instance.blueprintId())
                .bundleId(instance.bundleId()).instanceArn(instance.arn()).instanceName(instance.name())
                .isStaticIp(instance.isStaticIp()).keyPairName(instance.sshKeyName())
                .location(translateSDKtoLocation(instance.location())).privateIpAddress(instance.privateIpAddress())
                .publicIpAddress(instance.publicIpAddress())
                .resourceType(instance.resourceType() == null ? null : instance.resourceType().toString())
                .state(translateSDKtoState(instance.state())).sshKeyName(instance.sshKeyName())
                .supportCode(instance.supportCode()).tags(translateSDKtoTag(instance.tags()))
                .userName(instance.username()).hardware(tanslateSDKtoHardware(instance.hardware()))
                .networking(tanslateSDKtoNetworkiing(instance.networking())).build();
    }

    private static Networking tanslateSDKtoNetworkiing(InstanceNetworking networking) {
        return networking == null ? null : Networking.builder()
                .monthlyTransfer(tanslateSDKtoMonthlyTransfer(networking.monthlyTransfer()))
                .ports(networking.ports() == null ? null
                        : networking.ports().stream().map(Translator::tanslateSDKtoPort).collect(Collectors.toSet()))
                .build();
    }

    private static Port tanslateSDKtoPort(InstancePortInfo port) {
        return Port.builder().accessDirection(port.accessDirectionAsString()).accessType(port.accessTypeAsString())
                .commonName(port.commonName()).fromPort(port.fromPort()).toPort(port.toPort())
                .protocol(port.protocolAsString()).accessFrom(port.accessFrom())
                .accessDirection(port.accessDirectionAsString()).cidrListAliases(port.cidrListAliases())
                .cidrListAliases(port.cidrListAliases()).cidrs(port.cidrs()).ipv6Cidrs(port.ipv6Cidrs()).build();
    }

    private static MonthlyTransfer tanslateSDKtoMonthlyTransfer(
            software.amazon.awssdk.services.lightsail.model.MonthlyTransfer monthlyTransfer) {
        return MonthlyTransfer.builder().gbPerMonthAllocated(
                monthlyTransfer == null ? null : String.valueOf(monthlyTransfer.gbPerMonthAllocated())).build();
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
        return DeleteInstanceRequest.builder().instanceName(model.getInstanceName()).forceDeleteAddOns(true)
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
        return GetInstancesRequest.builder().pageToken(nextToken).build();
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
        val getInstancesResponce = (GetInstancesResponse) awsResponse;
        return getInstancesResponce.instances().stream().map(Translator::translateSDKInstanceToResourceModel)
                .collect(Collectors.toList());
    }

    private static Set<software.amazon.awssdk.services.lightsail.model.Tag> translateTagsToSdk(Collection<Tag> tags) {
        return tags == null ? null : tags.stream().map(tag -> software.amazon.awssdk.services.lightsail.model.Tag
                .builder().key(tag.getKey()).value(tag.getValue()).build()).collect(Collectors.toSet());
    }

    private static Set<Tag> translateSDKtoTag(List<software.amazon.awssdk.services.lightsail.model.Tag> tags) {
        return tags == null ? null : tags.stream().map(tag -> Tag.builder().key(tag.key()).value(tag.value()).build())
                .collect(Collectors.toSet());
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

    private static Hardware tanslateSDKtoHardware(InstanceHardware hardware) {
        return hardware == null ? null
                : Hardware.builder().cpuCount(hardware.cpuCount())
                        .ramSizeInGb(hardware.ramSizeInGb() == null ? 0 : Math.round(hardware.ramSizeInGb()))
                        .disks(tanslateSDKtoDisk(hardware.disks())).build();
    }

    private static Set<Disk> tanslateSDKtoDisk(List<software.amazon.awssdk.services.lightsail.model.Disk> disks) {
        return disks == null ? null : disks.stream()
                .filter(disk -> !disk.isSystemDisk())
                .map(disk -> Disk.builder().attachedTo(disk.attachedTo()).attachmentState(disk.attachmentState())
                        .isSystemDisk(disk.isSystemDisk()).iOPS(disk.iops()).path(disk.path()).diskName(disk.name())
                        .sizeInGb(String.valueOf(disk.sizeInGb())).build())
                .collect(Collectors.toSet());
    }

    private static Location translateSDKtoLocation(ResourceLocation location) {
        return location == null ? null : Location.builder().availabilityZone(location.availabilityZone())
                .regionName(location.regionNameAsString()).build();
    }

    private static State translateSDKtoState(InstanceState state) {
        return state == null ? null : State.builder().code(state.code()).name(state.name()).build();
    }

    public static EnableAddOnRequest translateToEnableAddOnRequest(ResourceModel resourceModel) {
        Optional<AddOn> autoSnapshotAddOn = Optional.empty();
        if (resourceModel.getAddOns() != null && resourceModel.getAddOns().size() > 0) {
            autoSnapshotAddOn = resourceModel.getAddOns().stream()
                    .filter(addOn -> AddOnType.AUTO_SNAPSHOT.toString().equalsIgnoreCase(addOn.getAddOnType()))
                    .findFirst();
        }
        return EnableAddOnRequest.builder().resourceName(resourceModel.getInstanceName())
                .addOnRequest(AddOnRequest.builder().addOnType(AddOnType.AUTO_SNAPSHOT.toString())
                        .autoSnapshotAddOnRequest(translateAutoSnapshotAddOnRequest(
                                autoSnapshotAddOn.map(AddOn::getAutoSnapshotAddOnRequest).orElse(null)))
                        .build())
                .build();
    }

    public static DisableAddOnRequest translateToDisableAddOnRequest(ResourceModel resourceModel) {
        return DisableAddOnRequest.builder().resourceName(resourceModel.getInstanceName())
                .addOnType(AddOnType.AUTO_SNAPSHOT.toString()).build();
    }

    /**
     * PutInstancePublicPorts API will always set accessType to public. in the other term, If customer asked to
     * close some ports instead if calling close instance public port then we will call with
     * PutInstancePublicPorts leaving out closing accessType private ones should be enough
     *
     * @param resourceModel
     *
     * @return
     */
    public static PutInstancePublicPortsRequest translateToPutInstancePublicPortRequest(ResourceModel resourceModel) {

        List<PortInfo> portInfo = new ArrayList<>();

        if (resourceModel.getNetworking() != null && resourceModel.getNetworking().getPorts() != null) {
            val publicPorts = resourceModel.getNetworking().getPorts().stream()
                    .filter(port -> !"private".equalsIgnoreCase(port.getAccessType())).collect(Collectors.toList());
            portInfo = publicPorts.stream().map(Translator::translateToSdkPort).collect(Collectors.toList());
        }

        return PutInstancePublicPortsRequest.builder().instanceName(resourceModel.getInstanceName()).portInfos(portInfo)
                .build();
    }

    private static PortInfo translateToSdkPort(Port port) {
        return PortInfo.builder().cidrListAliases(port.getCidrListAliases()).cidrs(port.getCidrs())
                .ipv6Cidrs(port.getIpv6Cidrs()).fromPort(port.getFromPort()).toPort(port.getToPort())
                .protocol(port.getProtocol()).build();
    }

    public static AttachDiskRequest translateToAttachDiskRequest(final String instanceName,
            final List<Disk> disksNeedAttachment) {
        if (disksNeedAttachment.size() == 0) {
            throw new IllegalArgumentException("There is no disk needs Attachment");
        }
        return AttachDiskRequest.builder().diskName(disksNeedAttachment.get(0).getDiskName()).instanceName(instanceName)
                .diskPath(disksNeedAttachment.get(0).getPath()).build();
    }

    public static DetachDiskRequest translateToDetachDiskRequest(final String instanceName,
            final List<Disk> disksNeedDetachment) {
        if (disksNeedDetachment.size() == 0) {
            throw new IllegalArgumentException("There is no disk needs Detachment");
        }
        return DetachDiskRequest.builder().diskName(disksNeedDetachment.get(0).getDiskName()).build();
    }

    public static Set<Disk> getDisksFromResourceModel(ResourceModel resourceModel) {
        return resourceModel.getHardware() == null ? ImmutableSet.of() : resourceModel.getHardware().getDisks();
    }

    public static StopInstanceRequest translateToSdkStopInstanceRequest(ResourceModel resourceModel) {
        return StopInstanceRequest.builder().instanceName(resourceModel.getInstanceName()).build();
    }

    public static StartInstanceRequest translateToSdkStartInstanceRequest(ResourceModel resourceModel) {
        return StartInstanceRequest.builder().instanceName(resourceModel.getInstanceName()).build();
    }
}
