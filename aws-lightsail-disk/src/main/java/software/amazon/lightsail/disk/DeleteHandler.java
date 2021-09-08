package software.amazon.lightsail.disk;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import software.amazon.lightsail.disk.helpers.handler.DiskHandler;

/**
 * Delete Handler will deal with deleting the Lightsail Disk created using the CloudFormation stack.
 */
public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
            final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        val diskHandler = new DiskHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(diskHandler::handleDelete).then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }
}
