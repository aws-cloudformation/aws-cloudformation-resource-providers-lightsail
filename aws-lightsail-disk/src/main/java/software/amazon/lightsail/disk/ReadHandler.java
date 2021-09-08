package software.amazon.lightsail.disk;

import com.google.common.collect.ImmutableList;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.disk.helpers.resource.Disk;

/**
 * ReadHandler to get particular Lightsail Disk.
 */
public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
            final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        this.logger = logger;
        val disk = new Disk(request.getDesiredResourceState(), logger, proxyClient, request);

        // STEP 1 [initialize a proxy context]
        return proxy
                .initiate("AWS-Lightsail-Disk::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                // STEP 2 [construct a body of a request]
                .translateToServiceRequest(Translator::translateToReadRequest)
                // STEP 3 [make an api call]
                .makeServiceCall((awsRequest, client) -> disk.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .done(awsResponse -> ProgressEvent
                        .defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }
}
