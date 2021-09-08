package software.amazon.lightsail.instance.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.ResourceType;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.CallbackContext;
import software.amazon.lightsail.instance.ResourceModel;
import software.amazon.lightsail.instance.Translator;
import software.amazon.lightsail.instance.helpers.resource.Instance;

import static software.amazon.lightsail.instance.BaseHandlerStd.InvalidInputException;
import static software.amazon.lightsail.instance.BaseHandlerStd.NotFoundException;
import static software.amazon.lightsail.instance.BaseHandlerStd.handleError;
import static software.amazon.lightsail.instance.CallbackContext.BACKOFF_DELAY;
import static software.amazon.lightsail.instance.CallbackContext.PRE_CHECK_CREATE;
import static software.amazon.lightsail.instance.CallbackContext.PRE_CHECK_DELETE;

@RequiredArgsConstructor
public class InstanceHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    /**
     * Check if Instance is already present, if Already present then create request will fail. We will call Get
     * Instances and check if we are getting NotFoundException. If no error is returned then Instance already present.
     */
    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {

        val instance = new Instance(resourceModel, logger, proxyClient, resourceModelRequest);
        if (callbackContext.getIsPreCheckDone(PRE_CHECK_CREATE)) {
            return progress;
        }
        logger.log("Executing AWS-Lightsail-Instance::Create::PreInstanceCheck...");
        return proxy
                .initiate("AWS-Lightsail-Instance::Create::PreInstanceCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).makeServiceCall((awsRequest, client) -> {
                    instance.read(awsRequest);
                    throw new CfnAlreadyExistsException(ResourceType.INSTANCE.toString(),
                            ((GetInstanceRequest) awsRequest).instanceName());
                }).handleError((awsRequest, exception, client, model, context) -> {
                    callbackContext.getIsPreCheckDone().put(PRE_CHECK_CREATE, true);
                    return handleError(exception, model, callbackContext,
                            ImmutableList.of(InvalidInputException, NotFoundException), logger,
                            this.getClass().getSimpleName());
                }).progress();
    }

    /**
     * Creation flow for the Instance. Transform the given resource model in to the create Instance request. Make
     * Lightsail create Instance call. If any Error then throw back that as the CloudFormation exception.
     */
    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> create(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val instance = new Instance(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Instance::InstanceCreate...");
        return proxy
                .initiate("AWS-Lightsail-Instance::InstanceCreate", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((awsRequest, client) -> {
                    return instance.create(awsRequest);
                }).stabilize((awsRequest, awsResponse, client, model, context) -> instance.isStabilizedCreate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    /**
     * Check if the Instance is present. If the Lightsail Instance is not present then it should have deleted outside of
     * the stack. Throw exceptions if there is any exception getting the stack.
     */
    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        val instance = new Instance(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Instance::Delete::PreInstanceDelete...");
        return proxy
                .initiate("AWS-Lightsail-Instance::Delete::PreInstanceDelete", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> instance.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model,
                        context) -> this.isStabilized(this.callbackContext, PRE_CHECK_DELETE)
                                || instance.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    /**
     * Make delete Instance call to delete Lightsail Instances. Stabilize and check if the Instance is gone.
     */
    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val instance = new Instance(resourceModel, logger, proxyClient, resourceModelRequest);
        logger.log("Executing AWS-Lightsail-Instance::InstanceDelete...");
        return proxy
                .initiate("AWS-Lightsail-Instance::InstanceDelete", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> instance.delete(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> instance.isStabilizedDelete())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        // Each resource of the Instance has its own update. Instance itself doesn't have its own update.
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        // Each resource of the Instance has its own update. Instance itself doesn't have its own update.
        throw new UnsupportedOperationException();
    }
}
