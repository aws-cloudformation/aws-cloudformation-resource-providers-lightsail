package software.amazon.lightsail.database;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.database.helpers.handler.DatabaseHandler;
import software.amazon.lightsail.database.helpers.resource.Database;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected DatabaseHandler getDatabaseHandler(final AmazonWebServicesClientProxy proxy,
                                                 final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                 final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new DatabaseHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
    }

    protected Database getDatabase(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Database(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    protected UpdateHandler getUpdateHandler() {
        return new UpdateHandler();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<LightsailClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        val databaseHandler = getDatabaseHandler(proxy, request, callbackContext, proxyClient, logger);
        val database = getDatabase(request, proxyClient, logger);

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext).then(progress -> {
            // NOTE: Availability Zone is not required on the model as we allow for this to be auto-picked
            ResourceModel resourceModel = progress.getResourceModel();
            if (StringUtils.isEmpty(resourceModel.getAvailabilityZone())) {
                logger.log("Picking the Availability Zone..");
                resourceModel.setAvailabilityZone(database.getFirstAvailabilityZone());
            }
            //During create, the update database should be applied immediately
            return ProgressEvent.progress(resourceModel, progress.getCallbackContext());
        }).then(databaseHandler::handleCreate).then(progress -> {
            // Always go via update handler. What ever not get done in create will be updated in update
            // Handler
            return getUpdateHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger);
        });
    }
}
