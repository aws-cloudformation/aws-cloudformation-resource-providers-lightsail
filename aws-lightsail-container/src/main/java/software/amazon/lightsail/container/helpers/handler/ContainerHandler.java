package software.amazon.lightsail.container.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetContainerServicesRequest;
import software.amazon.awssdk.services.lightsail.model.ResourceType;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.container.CallbackContext;
import software.amazon.lightsail.container.ResourceModel;
import software.amazon.lightsail.container.Translator;
import software.amazon.lightsail.container.helpers.resource.Container;

import static software.amazon.lightsail.container.BaseHandlerStd.*;
import static software.amazon.lightsail.container.CallbackContext.*;

@RequiredArgsConstructor
public class ContainerHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    protected Container getContainer(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Container(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val container = getContainer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Container::Update::PreCheck...");
        return proxy
                .initiate("AWS-Lightsail-Container::Update::PreCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> container.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        // Updating container and create container service deployments.
        return createContainerServiceDeployment(progress).then(this::updateContainer);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val container = getContainer(resourceModelRequest, proxyClient, logger);
        if (callbackContext.getIsPreCheckDone(PRE_CHECK_CREATE)) {
            return progress;
        }
        logger.log("Executing AWS-Lightsail-Container::Create::PreExistenceCheck...");
        return proxy
                .initiate("AWS-Lightsail-Container::Create::PreExistenceCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).makeServiceCall((awsRequest, client) -> {
                    container.read(awsRequest);
                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    throw new CfnAlreadyExistsException(ResourceType.CONTAINER_SERVICE.toString(),
                            ((GetContainerServicesRequest) awsRequest).serviceName());
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
        val container = getContainer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Container::Create...");
        return proxy
                .initiate("AWS-Lightsail-Container::Create", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((awsRequest, client) -> container.create(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> container.isStabilizedCreate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val container = getContainer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Container::Delete::PreDeletionCheck..");
        return proxy
                .initiate("AWS-Lightsail-Container::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> container.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> this.isStabilized(this.callbackContext, PRE_CHECK_DELETE) ||
                        container.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val container = getContainer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Container::Delete...");
        return proxy
                .initiate("AWS-Lightsail-Container::Delete", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> container.delete(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> this.isStabilized(this.callbackContext, POST_CHECK_DELETE) &&
                        container.isStabilizedDelete())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> updateContainer(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val container = getContainer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Container::Update...");
        return proxy
                .initiate("AWS-Lightsail-Container::Update", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> container.update(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> container.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> createContainerServiceDeployment(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val container = getContainer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Container::Update::DeployContainers...");
        return proxy
                .initiate("AWS-Lightsail-Container::Update::DeployContainers", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateContainerServiceDeploymentRequest)
                .makeServiceCall((awsRequest, client) -> container.createContainerServiceDeployment(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> container.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

}
