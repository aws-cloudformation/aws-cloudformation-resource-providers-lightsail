package software.amazon.lightsail.bucket;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.bucket.helpers.handler.BucketHandler;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected BucketHandler getBucketHandler(final AmazonWebServicesClientProxy proxy,
                                                 final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                 final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                          final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        val bucketHandler = getBucketHandler(proxy, request, callbackContext, proxyClient, logger);
        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(bucketHandler::handleDelete).then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }
}
