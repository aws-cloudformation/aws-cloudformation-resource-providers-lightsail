package software.amazon.lightsail.database;

import com.google.common.collect.ImmutableList;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.database.helpers.resource.Database;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected Database getDatabase(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Database(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<LightsailClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        val database = getDatabase(request, proxyClient, logger);

        return proxy
                .initiate("AWS-Lightsail-Database::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> database.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .done(awsResponse -> ProgressEvent
                        .defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }
}
