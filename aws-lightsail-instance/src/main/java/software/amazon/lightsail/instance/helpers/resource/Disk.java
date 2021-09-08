package software.amazon.lightsail.instance.helpers.resource;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetDiskRequest;
import software.amazon.awssdk.services.lightsail.model.GetDiskResponse;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.ResourceModel;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static software.amazon.lightsail.instance.Translator.getDisksFromResourceModel;
import static software.amazon.lightsail.instance.Translator.translateFromReadResponse;
import static software.amazon.lightsail.instance.Translator.translateToAttachDiskRequest;
import static software.amazon.lightsail.instance.Translator.translateToDetachDiskRequest;

/**
 * Helper class to handle Disk Interactions with the Instance resource.
 */
@RequiredArgsConstructor
public class Disk implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    public AwsResponse update(AwsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AwsResponse create(AwsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AwsResponse delete(AwsRequest request) {
        throw new UnsupportedOperationException();
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

    /**
     * Attach Disks that are there in the desired resource model but not there in the current resource model. We will
     * attach disk one by one.
     *
     * @return AwsResponse
     */
    public AwsResponse attachDisks() {
        logger.log(String.format("Attaching Disks to Instance: %s", resourceModel.getInstanceName()));
        AwsResponse awsResponse = null;
        Collection<software.amazon.lightsail.instance.Disk> disksNeedAttachment = this.disksNeedAttachment();
        while (disksNeedAttachment.size() > 0) {
            this.attach(disksNeedAttachment);
            disksNeedAttachment = this.disksNeedAttachment();
        }
        return awsResponse;
    }

    /**
     * Check if Disks need to be attached to the Instance. This is done by comparing the current ResourceModel with the
     * desired resourceModel. If the Desired resource models has more disks than the current resource model then we need
     * to attach disks.
     *
     * @return boolean
     */
    private boolean isAttachDiskNeeded() {

        // Get the disks that are there in the desired and not there in current disks
        // If attach disk is needed then
        return disksNeedAttachment().size() > 0;
    }

    /**
     * Check if current disk is free and not used by any Instance.
     *
     * @param disk
     *
     * @return boolean
     */
    private boolean isDiskFree(final software.amazon.awssdk.services.lightsail.model.Disk disk) {
        return disk != null && (disk.isAttached() == null || !disk.isAttached())
                && Strings.isNullOrEmpty(disk.attachedTo()) && "detached".equalsIgnoreCase(disk.attachmentState())
                && !"pending".equalsIgnoreCase(disk.state().toString());
    }

    public Collection<software.amazon.lightsail.instance.Disk> disksNeedAttachment() {
        ResourceModel desiredResourceModel = resourceModelRequest.getDesiredResourceState();
        ResourceModel currentResourceModel = getCurrentResourceModelFromLightsail();
        val disksNeedAttachment = getDisksNeedAttachment(getDisksFromResourceModel(currentResourceModel),
                getDisksFromResourceModel(desiredResourceModel));

        return disksNeedAttachment;
    }

    private ResourceModel getCurrentResourceModelFromLightsail() {
        return translateFromReadResponse(new Instance(resourceModel, logger, proxyClient, resourceModelRequest)
                .read(GetInstanceRequest.builder().instanceName(resourceModel.getInstanceName()).build()));
    }

    /**
     * Get the disk(any in case of many) that are there in the desired resource model but not there in the current
     * resource model. Wait for Instance not to be in the non terminal state and wait for disk to became free then
     * detach the disk.
     *
     * @return AwsResponse
     */
    private AwsResponse attach(Collection<software.amazon.lightsail.instance.Disk> disksNeedAttachment) {
        val attachRequest = translateToAttachDiskRequest(resourceModel.getInstanceName(),
                (List<software.amazon.lightsail.instance.Disk>) disksNeedAttachment);
        logger.log(String.format("Attaching Disk: %s to Instance: %s", attachRequest.diskName(),
                resourceModel.getInstanceName()));
        return proxyClient.injectCredentialsAndInvokeV2(attachRequest, proxyClient.client()::attachDisk);
    }

    /**
     * Get the disks that are there in the desired and not there in current disks
     *
     * @param currentDisks
     * @param desiredDisks
     *
     * @return
     */
    private Collection<software.amazon.lightsail.instance.Disk> getDisksNeedAttachment(
            Set<software.amazon.lightsail.instance.Disk> currentDisks,
            Set<software.amazon.lightsail.instance.Disk> desiredDisks) {
        return desiredDisks.stream().filter(disk -> {
            logger.log(String.format("Checking if Disk: %s needs attachment", disk.getDiskName()));
            return disk.getDiskName() != null && currentDisks.stream().noneMatch(curDisk -> {
                logger.log(String.format("Current Disk %s %s %s", curDisk.getDiskName(), curDisk.getAttachmentState(),
                        disk.getDiskName().equals(curDisk.getDiskName())));
                return curDisk.getDiskName() != null &&
                // check the disks that are in the attached or attaching state.
                (!"detached".equalsIgnoreCase(curDisk.getAttachmentState())
                        || !"detaching".equalsIgnoreCase(curDisk.getAttachmentState()))
                        && disk.getDiskName().equals(curDisk.getDiskName());
            });
        }).collect(Collectors.toList());
    }

    /**
     * Detach the disks that are not there in the desired resource model but in the current resource model. Instance
     * need to be in the stopped state for the disk to be detached from the Instance. We stop the Instance if
     * detachments needed and detach each disk one by one. Once the process is complete we start the instance again.
     *
     * @return AwsResponse
     */
    public AwsResponse detachDisks() {
        AwsResponse awsResponse = null;
        Collection<software.amazon.lightsail.instance.Disk> disksNeedDetachment = this.getDisksNeedDetachment();
        while (disksNeedDetachment.size() > 0) {
            this.detach(disksNeedDetachment);
            disksNeedDetachment = this.getDisksNeedDetachment();
        }
        return awsResponse;
    }

    /**
     * Get the disks that are there in the current and not there in desired disks
     *
     * @param currentDisks
     * @param desiredDisks
     *
     * @return
     */
    private static Collection<software.amazon.lightsail.instance.Disk> getDisksNeedDetachment(
            Set<software.amazon.lightsail.instance.Disk> currentDisks,
            Set<software.amazon.lightsail.instance.Disk> desiredDisks) {
        return currentDisks.stream().filter(disk -> disk.getDiskName() != null
                && ("attached".equalsIgnoreCase(disk.getAttachmentState())
                        || "attaching".equalsIgnoreCase(disk.getAttachmentState()))
                && desiredDisks.stream().noneMatch(
                        curDisk -> curDisk.getDiskName() != null && disk.getDiskName().equals(curDisk.getDiskName())))
                .collect(Collectors.toList());
    }

    /**
     * Check if we need to detach Disks from the Instance. This is done by comparing the current ResourceModel with the
     * desired resourceModel. If the Current ResourceModel has disks that are not there in the desired resource model
     * then we need to detach disks.
     *
     * @return boolean
     */
    public boolean isDetachDiskNeeded() {

        // Get the disks that are there in the current and not there in desired disks
        // If attach disk is needed then
        return getDisksNeedDetachment().size() > 0;
    }

    private Collection<software.amazon.lightsail.instance.Disk> getDisksNeedDetachment() {
        ResourceModel desiredResourceModel = resourceModelRequest.getDesiredResourceState();
        ResourceModel currentResourceModel = getCurrentResourceModelFromLightsail();
        val currentDisks = getDisksFromResourceModel(currentResourceModel);
        val desiredDisks = getDisksFromResourceModel(desiredResourceModel);
        // Get the disks that are there in the current and not there in desired disks
        // If attach disk is needed then
        return getDisksNeedDetachment(currentDisks, desiredDisks);
    }

    /**
     * Disks can be attached to the Instance only if its not in Use by any other Instance. We will continue to wait for
     * the Disk to became Free. If the disk never attained the free state then the update will be rolled back by
     * CloudFormation to go to the previous state.
     *
     * @param diskName
     */
    public boolean isDiskDetached(final String diskName) {
        val disk = ((GetDiskResponse) this.read(GetDiskRequest.builder().diskName(diskName).build())).disk();
        logger.log(String.format("Waiting for Disk: %s to be ready", disk.name()));
        return isDiskFree(disk);
    }

    /**
     * Get the disk(any in case of many) that are not there in the desired resource model but there in the current
     * resource model. Wait for Instance not to be in the non terminal state and then detach the disk.
     *
     * @return AwsResponse
     *
     * @param disksNeedDetachment
     */
    private AwsResponse detach(Collection<software.amazon.lightsail.instance.Disk> disksNeedDetachment) {
        val detachRequest = translateToDetachDiskRequest(resourceModel.getInstanceName(),
                (List<software.amazon.lightsail.instance.Disk>) disksNeedDetachment);
        logger.log(String.format("Detaching Disk: %s for Instance: %s", detachRequest.diskName(),
                resourceModel.getInstanceName()));
        val awsResponse = proxyClient.injectCredentialsAndInvokeV2(detachRequest, proxyClient.client()::detachDisk);
        return awsResponse;
    }
}
