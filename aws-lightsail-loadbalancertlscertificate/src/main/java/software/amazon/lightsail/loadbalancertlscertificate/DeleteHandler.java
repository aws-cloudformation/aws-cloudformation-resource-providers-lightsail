package software.amazon.lightsail.loadbalancertlscertificate;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.loadbalancertlscertificate.helpers.handler.LoadBalancerTlsCertificateHandler;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected LoadBalancerTlsCertificateHandler getLoadBalancerTlsCertificateHandler(final AmazonWebServicesClientProxy proxy,
                                                         final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                         final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                          final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        val loadBalancerTlsCertificateHandler = getLoadBalancerTlsCertificateHandler(proxy, request, callbackContext, proxyClient, logger);
        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(loadBalancerTlsCertificateHandler::handleDelete).then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }
}
