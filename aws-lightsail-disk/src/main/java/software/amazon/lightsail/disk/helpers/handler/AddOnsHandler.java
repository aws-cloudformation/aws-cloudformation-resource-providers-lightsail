package software.amazon.lightsail.disk.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.disk.CallbackContext;
import software.amazon.lightsail.disk.ResourceModel;
import software.amazon.lightsail.disk.Translator;
import software.amazon.lightsail.disk.helpers.resource.AddOns;
import software.amazon.lightsail.disk.helpers.resource.Disk;

import static software.amazon.lightsail.disk.BaseHandlerStd.handleError;
import static software.amazon.lightsail.disk.CallbackContext.BACKOFF_DELAY;
import static software.amazon.lightsail.disk.CallbackContext.PRE_ADDONS_UPDATE;

@RequiredArgsConstructor
public class AddOnsHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        val disk = new Disk(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Disk::Update::AddOnPreCheck...");
        return proxy
                .initiate("AWS-Lightsail-Disk::Update::AddOnPreCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> disk.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model,
                        context) -> this.isStabilized(this.callbackContext, PRE_ADDONS_UPDATE)
                                || disk.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        val addOn = new AddOns(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Disk::Update::AddOn...");
        return proxy
                .initiate("AWS-Lightsail-Disk::Update::AddOn", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((awsRequest, client) -> addOn.update(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> addOn.isStabilizedUpdate())
                .handleError((awsRequest, e, client, model, context) -> {
                    if (addOn.isSafeExceptionCreateOrUpdate(e)) {
                        return ProgressEvent.progress(progress.getResourceModel(), callbackContext);
                    } else {
                        return handleError(e, progress.getResourceModel(), callbackContext, ImmutableList.of(), logger,
                                this.getClass().getSimpleName());
                    }
                }).progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Delete Happening for AddOns Separately. Only Enable/Disable those will be taken care in update.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Delete Happening for AddOns Separately. Only Enable/Disable those will be taken care in update.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> create(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Create Happening for AddOns Separately.
        // We create Instance/Disk along with AddOns.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Create Happening for AddOns Separately.
        // We create Instance/Disk along with AddOns.
        throw new UnsupportedOperationException();
    }
}
