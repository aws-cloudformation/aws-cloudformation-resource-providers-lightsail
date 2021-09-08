package software.amazon.lightsail.instance.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetInstanceResponse;
import software.amazon.awssdk.services.lightsail.model.StopInstanceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.CallbackContext;
import software.amazon.lightsail.instance.ResourceModel;
import software.amazon.lightsail.instance.Translator;
import software.amazon.lightsail.instance.helpers.resource.Disk;
import software.amazon.lightsail.instance.helpers.resource.Instance;
import static software.amazon.lightsail.instance.BaseHandlerStd.handleError;
import static software.amazon.lightsail.instance.CallbackContext.BACKOFF_DELAY;
import static software.amazon.lightsail.instance.CallbackContext.PRE_CHECK_UPDATE_ATTACH;
import static software.amazon.lightsail.instance.CallbackContext.PRE_CHECK_UPDATE_DETACH;
import static software.amazon.lightsail.instance.CallbackContext.PRE_DISK_UPDATE;

@RequiredArgsConstructor
public class DiskHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val instance = new Instance(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Instance::Update::DiskPreCheck...");
        return proxy
                .initiate("AWS-Lightsail-Instance::Update::DiskPreCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> instance.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model,
                        context) -> this.isStabilized(this.callbackContext, PRE_DISK_UPDATE)
                                || instance.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        // do detach the disk before attach.
        return preDetachDisk(progress).then(this::detachDisk).then(this::postDetachDisk).then(this::preAttachDisk)
                .then(this::attachDisk);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        // Create/Delete for the disks won't happen here, it will happen in its own resource.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> create(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        // Create/Delete for the disks won't happen here, it will happen in its own resource.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        // Create/Delete for the disks won't happen here, it will happen in its own resource.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        // Create/Delete for the disks won't happen here, it will happen in its own resource.
        throw new UnsupportedOperationException();
    }

    /**
     * We need to stop the Instance before we detach the disks.
     *
     * @param progress
     *
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> preAttachDisk(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val disk = new Disk(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Instance::Update::PreDiskAttach...");
        return proxy
                .initiate("AWS-Lightsail-Instance::Update::PreDiskAttach", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> {
                    // Just return empty Get Response. We are not making any service call here.
                    // All we care out here is to make sure disks need to attach are in free state.
                    return GetInstanceResponse.builder().build();
                }).stabilize((awsRequest, awsResponse, client, model, context) -> {
                    val disksNeedAttachment = disk.disksNeedAttachment();
                    logger.log(String.format("Need to attach %s Disks. Checking if necessary disks are free",
                            disksNeedAttachment.size()));
                    for (val checkDisk : disksNeedAttachment) {
                        val diskFree = disk.isDiskDetached(checkDisk.getDiskName());
                        logger.log(String.format("%s disk current state(InUse=) is %s", checkDisk.getDiskName(),
                                diskFree));
                        if (!diskFree) {
                            // wait for max wait time and then return true.
                            return this.isStabilized(this.callbackContext, PRE_CHECK_UPDATE_ATTACH);
                        }
                    }
                    return true;
                }).handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    /**
     * Attach new disks that are there in desired resource model but not in the actual resource model.
     *
     * @param progress
     *
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> attachDisk(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val disk = new Disk(resourceModel, logger, proxyClient, resourceModelRequest);
        val instance = new Instance(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Instance::Update::DiskAttach...");
        return proxy
                .initiate("AWS-Lightsail-Instance::Update::DiskAttach", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> disk.attachDisks())
                .stabilize((awsRequest, awsResponse, client, model, context) -> instance.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    /**
     * We need to stop the Instance before we detach the disks.
     *
     * @param progress
     *
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> preDetachDisk(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val instance = new Instance(resourceModel, logger, proxyClient, resourceModelRequest);
        val disk = new Disk(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Instance::Update::PreDiskDetach...");
        return proxy
                .initiate("AWS-Lightsail-Instance::Update::PreDiskDetach", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToSdkStopInstanceRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> {
                    val needDetach = disk.isDetachDiskNeeded();
                    if (needDetach) {
                        logger.log("Disks need detachment. Stopping Instance...");
                        return instance.stop(awsRequest);
                    } else {
                        logger.log("No disk detachment needed.");
                    }
                    return StopInstanceResponse.builder().build();
                })
                .stabilize((awsRequest, awsResponse, client, model,
                        context) -> this.isStabilized(this.callbackContext, PRE_CHECK_UPDATE_DETACH)
                                || instance.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    /**
     * Detach disks that are not there in the desired resource model.
     *
     * @param progress
     *
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> detachDisk(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val disk = new Disk(resourceModel, logger, proxyClient, resourceModelRequest);
        val instance = new Instance(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Instance::Update::DiskDetach...");
        return proxy
                .initiate("AWS-Lightsail-Instance::Update::DiskDetach", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> disk.detachDisks())
                .stabilize((awsRequest, awsResponse, client, model, context) -> instance.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    /**
     * We need to start the instance back after detaching the disk. Make sure even if the disk was in stopped state
     * before we are going to start it now.
     *
     * @param progress
     *
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> postDetachDisk(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val instance = new Instance(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Instance::Update::PostDiskDetach...");
        return proxy
                .initiate("AWS-Lightsail-Instance::Update::PostDiskDetach", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToSdkStartInstanceRequest)
                .makeServiceCall((awsRequest, client) -> instance.start(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> instance.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }
}
