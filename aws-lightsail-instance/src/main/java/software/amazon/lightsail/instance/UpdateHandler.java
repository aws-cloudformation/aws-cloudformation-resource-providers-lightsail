package software.amazon.lightsail.instance;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.helpers.handler.AddOnsHandler;
import software.amazon.lightsail.instance.helpers.handler.DiskHandler;
import software.amazon.lightsail.instance.helpers.handler.NetworkHandler;
import software.amazon.lightsail.instance.helpers.handler.TagsHandler;

/**
 * Update Handler will deal with Updating Instances properties.
 */
public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
            final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        this.logger = logger;
        val addOnHandler = new AddOnsHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
        val diskHandler = new DiskHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
        val networkHandler = new NetworkHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
        val tagsHandler = new TagsHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(addOnHandler::handleUpdate).then(networkHandler::handleUpdate).then(diskHandler::handleUpdate)
                .then(tagsHandler::handleUpdate).then(progress -> new ReadHandler().handleRequest(proxy, request,
                        callbackContext, proxyClient, logger));
    }
}
