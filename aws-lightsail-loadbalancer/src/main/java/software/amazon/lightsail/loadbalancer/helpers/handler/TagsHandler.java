package software.amazon.lightsail.loadbalancer.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.loadbalancer.CallbackContext;
import software.amazon.lightsail.loadbalancer.ResourceModel;
import software.amazon.lightsail.loadbalancer.Translator;
import software.amazon.lightsail.loadbalancer.helpers.resource.LoadBalancer;
import software.amazon.lightsail.loadbalancer.helpers.resource.Tags;

import static software.amazon.lightsail.loadbalancer.BaseHandlerStd.handleError;


@RequiredArgsConstructor
public class TagsHandler extends ResourceHandler {

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

    protected Tags getTag(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Tags(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancer = getLoadBalancer(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancer::Update::PreTag...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancer::Update::PreTag", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> loadBalancer.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancer = getLoadBalancer(resourceModelRequest, proxyClient, logger);
        val tag = getTag(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancer::Update::Tag...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancer::Update::Tag", proxyClient, progress.getResourceModel(),
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
