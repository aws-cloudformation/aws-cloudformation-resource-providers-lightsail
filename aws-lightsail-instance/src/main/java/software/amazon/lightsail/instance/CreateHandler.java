package software.amazon.lightsail.instance;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.helpers.handler.InstanceHandler;
import software.amazon.lightsail.instance.helpers.resource.Instance;

/**
 * Create Handler does invoked for the creation flow of the LightSail Instance in CloudFormation Stack.
 */
public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
            final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        this.logger = logger;
        val instanceHandler = new InstanceHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
        val instance = new Instance(request.getDesiredResourceState(), logger, proxyClient, request);
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext).then(progress -> {
            // NOTE: Availability Zone is not required on the model as we allow for this to be auto-picked
            ResourceModel resourceModel = progress.getResourceModel();
            if (StringUtils.isEmpty(resourceModel.getAvailabilityZone())) {
                logger.log("Picking the Availability Zone..");
                resourceModel.setAvailabilityZone(instance.getFirstAvailabilityZone());
            }
            return ProgressEvent.progress(resourceModel, progress.getCallbackContext());
        }).then(instanceHandler::handleCreate).then(progress -> {
            // Always go via update handler. What ever not get done in create will be updated in update
            // Handler
            return new UpdateHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger);
        });
    }
}
