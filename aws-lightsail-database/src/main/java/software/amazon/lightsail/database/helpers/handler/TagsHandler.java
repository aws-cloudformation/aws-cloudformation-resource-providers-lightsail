package software.amazon.lightsail.database.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.database.CallbackContext;
import software.amazon.lightsail.database.ResourceModel;
import software.amazon.lightsail.database.Translator;
import software.amazon.lightsail.database.helpers.resource.Database;
import software.amazon.lightsail.database.helpers.resource.Tags;

import static software.amazon.lightsail.database.BaseHandlerStd.handleError;
import static software.amazon.lightsail.database.CallbackContext.BACKOFF_DELAY;
import static software.amazon.lightsail.database.CallbackContext.PRE_TAG_UPDATE;

@RequiredArgsConstructor
public class TagsHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    protected Database getDatabase(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Database(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    protected Tags getTag(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Tags(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        val database = getDatabase(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Database::Update::PreTag...");
        return proxy
                .initiate("AWS-Lightsail-Database::Update::PreTag", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> database.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model,
                        context) -> this.isStabilized(this.callbackContext, PRE_TAG_UPDATE)
                                || database.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        val database = getDatabase(resourceModelRequest, proxyClient, logger);
        val tag = getTag(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Database::Update::Tag...");
        return proxy
                .initiate("AWS-Lightsail-Database::Update::Tag", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> tag.update(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> database.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Create/Delete for Tags. We do everything in update.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> create(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Create/Delete for Tags. We do everything in update.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Create/Delete for Tags. We do everything in update.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Create/Delete for Tags. We do everything in update.
        throw new UnsupportedOperationException();
    }
}
