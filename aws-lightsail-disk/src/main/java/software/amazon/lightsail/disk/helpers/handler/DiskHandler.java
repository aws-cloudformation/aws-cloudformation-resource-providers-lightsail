package software.amazon.lightsail.disk.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetDiskRequest;
import software.amazon.awssdk.services.lightsail.model.ResourceType;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.disk.CallbackContext;
import software.amazon.lightsail.disk.ResourceModel;
import software.amazon.lightsail.disk.Translator;
import software.amazon.lightsail.disk.helpers.resource.Disk;

import static software.amazon.lightsail.disk.BaseHandlerStd.InvalidInputException;
import static software.amazon.lightsail.disk.BaseHandlerStd.NotFoundException;
import static software.amazon.lightsail.disk.BaseHandlerStd.handleError;
import static software.amazon.lightsail.disk.CallbackContext.BACKOFF_DELAY;
import static software.amazon.lightsail.disk.CallbackContext.PRE_CHECK_CREATE;
import static software.amazon.lightsail.disk.CallbackContext.PRE_CHECK_DELETE;

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
        // Disk will not be updated.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        // Disk will not be updated.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        // Create/Delete for the disks won't happen here, it will happen in its own resource.
        val disk = new Disk(resourceModel, logger, proxyClient, resourceModelRequest);
        if (callbackContext.getIsPreCheckDone(PRE_CHECK_CREATE)) {
            return progress;
        }
        logger.log("Executing AWS-Lightsail-Disk::Create::PreExistanceCheck...");
        return proxy
                .initiate("AWS-Lightsail-Disk::Create::PreExistanceCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).makeServiceCall((awsRequest, client) -> {
                    disk.read(awsRequest);
                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    throw new CfnAlreadyExistsException(ResourceType.DISK.toString(),
                            ((GetDiskRequest) awsRequest).diskName());
                }).handleError((awsRequest, exception, client, model, context) -> {
                    callbackContext.getIsPreCheckDone().put(PRE_CHECK_CREATE, true);
                    return handleError(exception, model, callbackContext,
                            ImmutableList.of(InvalidInputException, NotFoundException), logger,
                            this.getClass().getSimpleName());
                }).progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> create(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val disk = new Disk(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Disk::Create...");
        return proxy
                .initiate("AWS-Lightsail-Disk::Create", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((awsRequest, client) -> disk.create(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> disk.isStabilizedCreate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val disk = new Disk(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Disk::Delete::PreDeletionCheck..");
        return proxy
                .initiate("AWS-Lightsail-Disk::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> disk.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    if (disk.isDiskFree()) {
                        return true;
                    }
                    return this.isStabilized(this.callbackContext, PRE_CHECK_DELETE);
                }).handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val disk = new Disk(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Disk::Delete...");
        return proxy
                .initiate("AWS-Lightsail-Disk::Delete", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> disk.delete(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> disk.isStabilizedDelete())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }
}
