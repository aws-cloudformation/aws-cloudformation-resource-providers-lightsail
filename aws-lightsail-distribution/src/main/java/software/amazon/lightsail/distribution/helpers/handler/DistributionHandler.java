package software.amazon.lightsail.distribution.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetDistributionsRequest;
import software.amazon.awssdk.services.lightsail.model.ResourceType;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.distribution.CallbackContext;
import software.amazon.lightsail.distribution.ResourceModel;
import software.amazon.lightsail.distribution.Translator;
import software.amazon.lightsail.distribution.helpers.resource.Distribution;

import static software.amazon.lightsail.distribution.BaseHandlerStd.*;
import static software.amazon.lightsail.distribution.CallbackContext.*;

@RequiredArgsConstructor
public class DistributionHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    protected Distribution getDistribution(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Distribution(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val distribution = getDistribution(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Distribution::Update::PreCheck...");
        return proxy
                .initiate("AWS-Lightsail-Distribution::Update::PreCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> distribution.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> distribution.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        // Updating distribution, distribution bundle, and detaching/attaching certificates.
        return updateDistribution(progress).then(this::updateDistributionBundle)
                .then(this::detachCertificate).then(this::attachCertificate);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val distribution = getDistribution(resourceModelRequest, proxyClient, logger);
        if (callbackContext.getIsPreCheckDone(PRE_CHECK_CREATE)) {
            return progress;
        }
        logger.log("Executing AWS-Lightsail-Distribution::Create::PreExistenceCheck...");
        return proxy
                .initiate("AWS-Lightsail-Distribution::Create::PreExistenceCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).makeServiceCall((awsRequest, client) -> {
                    distribution.read(awsRequest);
                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    throw new CfnAlreadyExistsException(ResourceType.DISTRIBUTION.toString(),
                            ((GetDistributionsRequest) awsRequest).distributionName());
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
        val distribution = getDistribution(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Distribution::Create...");
        return proxy
                .initiate("AWS-Lightsail-Distribution::Create", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((awsRequest, client) -> distribution.create(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> distribution.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val distribution = getDistribution(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Distribution::Delete::PreDeletionCheck..");
        return proxy
                .initiate("AWS-Lightsail-Distribution::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> distribution.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> this.isStabilized(this.callbackContext, PRE_CHECK_DELETE) ||
                        distribution.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val distribution = getDistribution(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Distribution::Delete...");
        return proxy
                .initiate("AWS-Lightsail-Distribution::Delete", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> distribution.delete(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> distribution.isStabilizedDelete())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> updateDistribution(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val distribution = getDistribution(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Distribution::Update...");
        return proxy
                .initiate("AWS-Lightsail-Distribution::Update", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToUpdateRequest)
                .makeServiceCall((awsRequest, client) -> distribution.update(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> distribution.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> updateDistributionBundle(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val distribution = getDistribution(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Distribution::Update::Bundle...");
        return proxy
                .initiate("AWS-Lightsail-Distribution::Update::Bundle", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToUpdateBundleRequest)
                .makeServiceCall((awsRequest, client) -> distribution.updateBundle(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> distribution.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> detachCertificate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val distribution = getDistribution(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Distribution::DetachCertificate...");
        return proxy
                .initiate("AWS-Lightsail-Distribution::DetachCertificate", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDetachCertificateRequest)
                .makeServiceCall((awsRequest, client) -> distribution.detachCertificate(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> distribution.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> attachCertificate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val distribution = getDistribution(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Distribution::AttachCertificate...");
        return proxy
                .initiate("AWS-Lightsail-Distribution::AttachCertificate", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToAttachCertificateRequest)
                .makeServiceCall((awsRequest, client) -> distribution.attachCertificate(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> distribution.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }
}
