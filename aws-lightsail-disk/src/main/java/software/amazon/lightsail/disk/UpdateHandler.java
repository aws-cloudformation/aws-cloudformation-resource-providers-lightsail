package software.amazon.lightsail.disk;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.disk.helpers.handler.AddOnsHandler;
import software.amazon.lightsail.disk.helpers.handler.TagsHandler;

/**
 * Update Handler will deal with Updating Disks properties.
 */
public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
            final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        this.logger = logger;
        val addOnHandler = new AddOnsHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
        val tagsHandler = new TagsHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(addOnHandler::handleUpdate).then(tagsHandler::handleUpdate).then(progress -> new ReadHandler()
                        .handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
