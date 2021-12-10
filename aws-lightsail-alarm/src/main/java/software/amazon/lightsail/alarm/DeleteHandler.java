package software.amazon.lightsail.alarm;

import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.alarm.helpers.handler.AlarmHandler;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected AlarmHandler getAlarmHandler(final AmazonWebServicesClientProxy proxy,
                                           final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                           final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new AlarmHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                          final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        val alarmHandler = getAlarmHandler(proxy, request, callbackContext, proxyClient, logger);
        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(alarmHandler::handleDelete).then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }
}
