package software.amazon.lightsail.instance.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.CallbackContext;
import software.amazon.lightsail.instance.ResourceModel;
import software.amazon.lightsail.instance.Translator;
import software.amazon.lightsail.instance.helpers.resource.Instance;
import software.amazon.lightsail.instance.helpers.resource.Tags;

import static software.amazon.lightsail.instance.BaseHandlerStd.handleError;
import static software.amazon.lightsail.instance.CallbackContext.BACKOFF_DELAY;
import static software.amazon.lightsail.instance.CallbackContext.PRE_TAG_UPDATE;

@RequiredArgsConstructor
public class TagsHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        val instance = new Instance(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Instance::Update::PreTag...");
        return proxy
                .initiate("AWS-Lightsail-Instance::Update::PreTag", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> instance.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model,
                        context) -> this.isStabilized(this.callbackContext, PRE_TAG_UPDATE)
                                || instance.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        val instance = new Instance(resourceModel, logger, proxyClient, resourceModelRequest);
        val tag = new Tags(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Instance::Update::Tag...");
        return proxy
                .initiate("AWS-Lightsail-Instance::Update::Tag", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> tag.update(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> instance.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Create/Delete for Tags. We do everything in update Add or Remove ports.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> create(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Create/Delete for Tags. We do everything in update Add or Remove ports.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Create/Delete for Tags. We do everything in update Add or Remove ports.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Create/Delete for Tags. We do everything in update Add or Remove ports.
        throw new UnsupportedOperationException();
    }
}
