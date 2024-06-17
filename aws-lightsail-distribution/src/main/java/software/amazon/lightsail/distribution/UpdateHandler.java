package software.amazon.lightsail.distribution;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.distribution.helpers.handler.DistributionHandler;
import software.amazon.lightsail.distribution.helpers.handler.TagsHandler;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected TagsHandler getTagHandler(final AmazonWebServicesClientProxy proxy,
                                        final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                        final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new TagsHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
    }

    protected DistributionHandler getDistributionHandler(final AmazonWebServicesClientProxy proxy,
                                             final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                             final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
    }

    protected ReadHandler getReadHandler() {
        return new ReadHandler();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                          final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        this.logger = logger;

        val tagsHandler = getTagHandler(proxy, request, callbackContext, proxyClient, logger);
        val distributionHandler = getDistributionHandler(proxy, request, callbackContext, proxyClient, logger);

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(distributionHandler::handleUpdate).then(tagsHandler::handleUpdate).then(progress -> getReadHandler()
                        .handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}