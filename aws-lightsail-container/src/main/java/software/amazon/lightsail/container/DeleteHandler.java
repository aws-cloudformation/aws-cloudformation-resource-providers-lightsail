package software.amazon.lightsail.container;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.container.helpers.handler.ContainerHandler;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ContainerHandler getContainerHandler(final AmazonWebServicesClientProxy proxy,
                                                         final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                         final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new ContainerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                          final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        val containerHandler = getContainerHandler(proxy, request, callbackContext, proxyClient, logger);
        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(containerHandler::handleDelete).then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }
}
