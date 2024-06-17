package software.amazon.lightsail.staticip.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetStaticIpRequest;
import software.amazon.awssdk.services.lightsail.model.ResourceType;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.staticip.CallbackContext;
import software.amazon.lightsail.staticip.ResourceModel;
import software.amazon.lightsail.staticip.Translator;
import software.amazon.lightsail.staticip.helpers.resource.Instance;
import software.amazon.lightsail.staticip.helpers.resource.StaticIp;

import static software.amazon.lightsail.staticip.BaseHandlerStd.*;
import static software.amazon.lightsail.staticip.CallbackContext.*;

@RequiredArgsConstructor
public class StaticIpHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    protected StaticIp getStaticIp(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new StaticIp(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    protected Instance getInstance(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Instance(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val staticIp = getStaticIp(resourceModelRequest, proxyClient, logger);
        val instance = getInstance(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-StaticIp::Update::PreCheck...");
        return proxy
                .initiate("AWS-Lightsail-StaticIp::Update::PreCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> staticIp.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model,
                            context) -> this.isStabilized(this.callbackContext, PRE_CHECK_UPDATE)
                        || instance.isStabilized())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val staticIp = getStaticIp(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-StaticIp::Update...");
        return proxy
                .initiate("AWS-Lightsail-StaticIp::Update", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((awsRequest, client) -> staticIp.update(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val staticIp = getStaticIp(resourceModelRequest, proxyClient, logger);
        if (callbackContext.getIsPreCheckDone(PRE_CHECK_CREATE)) {
            return progress;
        }
        logger.log("Executing AWS-Lightsail-StaticIp::Create::PreExistenceCheck...");
        return proxy
                .initiate("AWS-Lightsail-StaticIp::Create::PreExistenceCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).makeServiceCall((awsRequest, client) -> {
                    staticIp.read(awsRequest);
                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    throw new CfnAlreadyExistsException(ResourceType.STATIC_IP.toString(),
                            ((GetStaticIpRequest) awsRequest).staticIpName());
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
        val staticIp = getStaticIp(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-StaticIp::Create...");
        return proxy
                .initiate("AWS-Lightsail-StaticIp::Create", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> staticIp.create(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> this.isStabilized(callbackContext, POST_CHECK_CREATE) &&
                        staticIp.isStabilizedCreate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val staticIp = getStaticIp(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-StaticIp::Delete::PreDeletionCheck..");
        return proxy
                .initiate("AWS-Lightsail-StaticIp::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> staticIp.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val staticIp = getStaticIp(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-StaticIp::Delete...");
        return proxy
                .initiate("AWS-Lightsail-StaticIp::Delete", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> staticIp.delete(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> staticIp.isStabilizedDelete())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

}