package software.amazon.lightsail.bucket;

import com.google.common.collect.ImmutableList;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.bucket.helpers.resource.Bucket;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected Bucket getBucket(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Bucket(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<LightsailClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        val bucket = getBucket(request, proxyClient, logger);

        return proxy
                .initiate("AWS-Lightsail-Bucket::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> bucket.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .done(awsResponse -> ProgressEvent
                        .defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }
}
