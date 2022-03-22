package software.amazon.lightsail.certificate.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.ResourceType;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.certificate.CallbackContext;
import software.amazon.lightsail.certificate.ResourceModel;
import software.amazon.lightsail.certificate.Translator;
import software.amazon.lightsail.certificate.helpers.resource.Certificate;

import static software.amazon.lightsail.certificate.BaseHandlerStd.*;
import static software.amazon.lightsail.certificate.CallbackContext.*;

@RequiredArgsConstructor
public class CertificateHandler extends ResourceHandler {

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

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val certificate = getCertificate(resourceModelRequest, proxyClient, logger);
        if (callbackContext.getIsPreCheckDone(PRE_CHECK_CREATE)) {
            return progress;
        }
        logger.log("Executing AWS-Lightsail-Certificate::Create::PreExistenceCheck...");
        return proxy
                .initiate("AWS-Lightsail-Certificate::Create::PreExistenceCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).makeServiceCall((awsRequest, client) -> {
                    certificate.read(awsRequest);
                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    throw new CfnAlreadyExistsException(ResourceType.CERTIFICATE.toString(),
                            resourceModel.getCertificateName());
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
        val certificate = getCertificate(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Certificate::Create...");
        return proxy
                .initiate("AWS-Lightsail-Certificate::Create", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((awsRequest, client) -> certificate.create(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> certificate.isStabilizedCreate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val certificate = getCertificate(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Certificate::Delete::PreDeletionCheck..");
        return proxy
                .initiate("AWS-Lightsail-Certificate::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> certificate.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val certificate = getCertificate(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Certificate::Delete...");
        return proxy
                .initiate("AWS-Lightsail-Certificate::Delete", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> certificate.delete(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> certificate.isStabilizedDelete())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Update for Tags. We do everything in update.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // No Update for Tags. We do everything in update.
        throw new UnsupportedOperationException();
    }

}
