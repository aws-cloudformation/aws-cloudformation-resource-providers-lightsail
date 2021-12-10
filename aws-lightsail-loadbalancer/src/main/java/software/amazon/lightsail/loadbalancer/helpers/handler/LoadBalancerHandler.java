package software.amazon.lightsail.loadbalancer.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.loadbalancer.CallbackContext;
import software.amazon.lightsail.loadbalancer.ResourceModel;
import software.amazon.lightsail.loadbalancer.Translator;
import software.amazon.lightsail.loadbalancer.helpers.resource.LoadBalancer;
import software.amazon.lightsail.loadbalancer.helpers.resource.Instance;

import java.util.Set;

import static software.amazon.lightsail.loadbalancer.BaseHandlerStd.*;
import static software.amazon.lightsail.loadbalancer.CallbackContext.*;

@RequiredArgsConstructor
public class LoadBalancerHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    protected LoadBalancer getLoadBalancer(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new LoadBalancer(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    protected Instance getInstance(final ResourceHandlerRequest<ResourceModel> request,
                               final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Instance(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancer = getLoadBalancer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancer::Update::PreCheck...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancer::Update::PreCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> loadBalancer.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        return updateLoadBalancerAttributes(progress).then(this::detachInstances)
                .then(this::preAttachInstances).then(this::attachInstances);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancer = getLoadBalancer(resourceModelRequest, proxyClient, logger);
        if (callbackContext.getIsPreCheckDone(PRE_CHECK_CREATE)) {
            return progress;
        }
        logger.log("Executing AWS-Lightsail-LoadBalancer::Create::PreExistenceCheck...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancer::Create::PreExistenceCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).makeServiceCall((awsRequest, client) -> {
                    loadBalancer.read(awsRequest);
                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    throw new CfnAlreadyExistsException(ResourceType.LOAD_BALANCER.toString(),
                            ((GetLoadBalancerRequest) awsRequest).loadBalancerName());
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
        val loadBalancer = getLoadBalancer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancer::Create...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancer::Create", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((awsRequest, client) -> loadBalancer.create(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancer = getLoadBalancer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancer::Delete::PreDeletionCheck..");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancer::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> loadBalancer.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancer = getLoadBalancer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancer::Delete...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancer::Delete", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> loadBalancer.delete(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> loadBalancer.isStabilizedDelete())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> updateLoadBalancerAttributes(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancer = getLoadBalancer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancer::Update::Attributes...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancer::Update::Attributes", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> loadBalancer.updateAttributes(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> detachInstances(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancer = getLoadBalancer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancer::Update::DetachInstances...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancer::Update::DetachInstances", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> loadBalancer.detachInstances(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> this.isStabilized(callbackContext, POST_DETACH_WAIT) &&
                        (this.isStabilized(callbackContext, POST_CHECK_DETACH) || loadBalancer.isStabilizedInstances()))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> preAttachInstances(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancer = getLoadBalancer(resourceModelRequest, proxyClient, logger);
        val instance = getInstance(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancer::Update::PreAttachInstances...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancer::Update::PreAttachInstances", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> {
                    // Just return empty Get Response. We are not making any service call here.
                    // We are making sure in the stabilize step that all instances that need to be attached are running.
                    return GetLoadBalancerResponse.builder().build();
                })
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    Set<String> desiredInstances = resourceModelRequest.getDesiredResourceState().getAttachedInstances();
                    Set<String> currentInstances = loadBalancer.getCurrentResourceModelFromLightsail().getAttachedInstances();

                    Set<String> resourcesToAttach = loadBalancer.setDifference(desiredInstances, currentInstances);

                    for (val resource: resourcesToAttach) {
                        if (!instance.isStabilized(resource)) {
                            return this.isStabilized(callbackContext, PRE_CHECK_ATTACH);
                        }
                    }
                    return true;
                })
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> attachInstances(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancer = getLoadBalancer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancer::Update::AttachInstances...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancer::Update::AttachInstances", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> loadBalancer.attachInstances(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> this.isStabilized(callbackContext, POST_ATTACH_WAIT) &&
                        (this.isStabilized(callbackContext, POST_CHECK_ATTACH) || loadBalancer.isStabilizedInstances()))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

}
