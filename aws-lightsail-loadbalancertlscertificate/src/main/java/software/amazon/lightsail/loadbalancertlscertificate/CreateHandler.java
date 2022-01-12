package software.amazon.lightsail.loadbalancertlscertificate;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.loadbalancertlscertificate.helpers.handler.LoadBalancerTlsCertificateHandler;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected LoadBalancerTlsCertificateHandler getLoadBalancerTlsCertificateHandler(final AmazonWebServicesClientProxy proxy,
                                                         final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                         final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
    }

    protected UpdateHandler getUpdateHandler() {
        return new UpdateHandler();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<LightsailClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        val loadBalancerTlsCertificateHandler = getLoadBalancerTlsCertificateHandler(proxy, request, callbackContext, proxyClient, logger);

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(loadBalancerTlsCertificateHandler::handleCreate).then(progress -> {
                    // Always go via update handler. What ever not get done in create will be updated in update
                    // Handler
                    return getUpdateHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger);
                });
    }
}
