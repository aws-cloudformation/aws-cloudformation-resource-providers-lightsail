package software.amazon.lightsail.instance;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.helpers.handler.InstanceHandler;

/**
 * Delete Handler will deal with deleting the Lightsail Instance created using the CloudFormation stack.
 */
public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
            final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        val instanceHandler = new InstanceHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(instanceHandler::handleDelete).then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }
}
