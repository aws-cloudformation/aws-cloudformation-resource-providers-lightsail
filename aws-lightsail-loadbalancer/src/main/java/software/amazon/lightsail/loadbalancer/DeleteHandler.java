package software.amazon.lightsail.loadbalancer;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.loadbalancer.helpers.handler.LoadBalancerHandler;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected LoadBalancerHandler getLoadBalancerHandler(final AmazonWebServicesClientProxy proxy,
                                             final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                             final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                          final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        val loadBalancerHandler = getLoadBalancerHandler(proxy, request, callbackContext, proxyClient, logger);
        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(loadBalancerHandler::handleDelete).then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }
}
