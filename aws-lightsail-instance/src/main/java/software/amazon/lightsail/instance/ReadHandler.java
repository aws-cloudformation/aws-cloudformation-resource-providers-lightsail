package software.amazon.lightsail.instance;

import com.google.common.collect.ImmutableList;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.helpers.resource.Instance;

/**
 * ReadHandler to get particular Lightsail Instance.
 */
public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
            final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        val instance = new Instance(request.getDesiredResourceState(), logger, proxyClient, request);
        this.logger = logger;
        return proxy
                .initiate("AWS-Lightsail-Instance::Read", proxyClient, request.getDesiredResourceState(),
                        callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> instance.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .done(awsResponse -> ProgressEvent
                        .defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }
}
