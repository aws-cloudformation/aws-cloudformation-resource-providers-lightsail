package software.amazon.lightsail.certificate.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.certificate.CallbackContext;
import software.amazon.lightsail.certificate.ResourceModel;
import software.amazon.lightsail.certificate.Translator;
import software.amazon.lightsail.certificate.helpers.resource.Certificate;
import software.amazon.lightsail.certificate.helpers.resource.Tags;

import static software.amazon.lightsail.certificate.BaseHandlerStd.handleError;


@RequiredArgsConstructor
public class TagsHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    protected Certificate getCertificate(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Certificate(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    protected Tags getTag(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Tags(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        val certificate = getCertificate(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Certificate::Update::PreTag...");
        return proxy
                .initiate("AWS-Lightsail-Certificate::Update::PreTag", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> certificate.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        val certificate = getCertificate(resourceModelRequest, proxyClient, logger);
        val tag = getTag(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Certificate::Update::Tag...");
        return proxy
                .initiate("AWS-Lightsail-Certificate::Update::Tag", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> tag.update(awsRequest))
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
