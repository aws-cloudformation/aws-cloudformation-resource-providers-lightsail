package software.amazon.lightsail.alarm.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetAlarmsRequest;
import software.amazon.awssdk.services.lightsail.model.ResourceType;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.alarm.CallbackContext;
import software.amazon.lightsail.alarm.ResourceModel;
import software.amazon.lightsail.alarm.Translator;
import software.amazon.lightsail.alarm.helpers.resource.Alarm;

import static software.amazon.lightsail.alarm.BaseHandlerStd.*;
import static software.amazon.lightsail.alarm.BaseHandlerStd.handleError;
import static software.amazon.lightsail.alarm.CallbackContext.PRE_CHECK_CREATE;

@RequiredArgsConstructor
public class AlarmHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    protected Alarm getAlarm(final ResourceHandlerRequest<ResourceModel> request,
                                                                       final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Alarm(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val alarm = getAlarm(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Alarm::Update::PreCheck...");
        return proxy
                .initiate("AWS-Lightsail-Alarm::Update::PreCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> alarm.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val alarm = getAlarm(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Alarm::Update...");
        return proxy
                .initiate("AWS-Lightsail-Alarm::Update", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToUpdateRequest)
                .makeServiceCall((awsRequest, client) -> alarm.update(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val alarm = getAlarm(resourceModelRequest, proxyClient, logger);
        if (callbackContext.getIsPreCheckDone(PRE_CHECK_CREATE)) {
            return progress;
        }
        logger.log("Executing AWS-Lightsail-Alarm::Create::PreExistenceCheck...");
        return proxy
                .initiate("AWS-Lightsail-Alarm::Create::PreExistenceCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).makeServiceCall((awsRequest, client) -> {
                    alarm.read(awsRequest);
                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    throw new CfnAlreadyExistsException(ResourceType.ALARM.toString(),
                            ((GetAlarmsRequest) awsRequest).alarmName());
                }).handleError((awsRequest, exception, client, model, context) -> {
                    callbackContext.getIsPreCheckDone().put(PRE_CHECK_CREATE, true);
                    return handleError(exception, model, callbackContext,
                            ImmutableList.of(InvalidInputException, NotFoundException), logger,
                            this.getClass().getSimpleName());
                }).progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> create(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val alarm = getAlarm(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Alarm::Create...");
        return proxy
                .initiate("AWS-Lightsail-Alarm::Create", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((awsRequest, client) -> alarm.create(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> alarm.isStabilizedCreate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val alarm = getAlarm(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Alarm::Delete::PreDeletionCheck..");
        return proxy
                .initiate("AWS-Lightsail-Alarm::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> alarm.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val alarm = getAlarm(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Alarm::Delete...");
        return proxy
                .initiate("AWS-Lightsail-Alarm::Delete", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> alarm.delete(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> alarm.isStabilizedDelete())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

}
