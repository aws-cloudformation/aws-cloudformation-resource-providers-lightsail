package software.amazon.lightsail.database.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetRelationalDatabaseRequest;
import software.amazon.awssdk.services.lightsail.model.ResourceType;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.database.CallbackContext;
import software.amazon.lightsail.database.ResourceModel;
import software.amazon.lightsail.database.Translator;
import software.amazon.lightsail.database.helpers.resource.Database;

import static software.amazon.lightsail.database.BaseHandlerStd.*;
import static software.amazon.lightsail.database.CallbackContext.*;

@RequiredArgsConstructor
public class DatabaseHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    protected Database getDatabase(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Database(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val database = getDatabase(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Database::Update::PreCheck...");
        return proxy
                .initiate("AWS-Lightsail-Database::Update::PreCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> database.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model,
                            context) -> this.isStabilized(this.callbackContext, PRE_CHECK_UPDATE)
                        || database.isStabilizedCreate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        // Updating database and database parameters
        return updateDatabase(progress).then(this::updateDatabaseParameters);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val database = getDatabase(resourceModelRequest, proxyClient, logger);
        if (callbackContext.getIsPreCheckDone(PRE_CHECK_CREATE)) {
            return progress;
        }
        logger.log("Executing AWS-Lightsail-Database::Create::PreExistenceCheck...");
        return proxy
                .initiate("AWS-Lightsail-Database::Create::PreExistenceCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).makeServiceCall((awsRequest, client) -> {
                    database.read(awsRequest);
                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    throw new CfnAlreadyExistsException(ResourceType.RELATIONAL_DATABASE.toString(),
                            ((GetRelationalDatabaseRequest) awsRequest).relationalDatabaseName());
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
        val database = getDatabase(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Database::Create...");
        return proxy
                .initiate("AWS-Lightsail-Database::Create", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((awsRequest, client) -> database.create(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> database.isStabilizedCreate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val database = getDatabase(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Database::Delete::PreDeletionCheck..");
        return proxy
                .initiate("AWS-Lightsail-Database::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> database.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> this.isStabilized(this.callbackContext, PRE_CHECK_DELETE) ||
                        database.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val database = getDatabase(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Database::Delete...");
        return proxy
                .initiate("AWS-Lightsail-Database::Delete", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> database.delete(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> database.isStabilizedDelete())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> updateDatabase(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val database = getDatabase(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Database::Update...");
        return proxy
                .initiate("AWS-Lightsail-Database::Update", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToUpdateRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> database.update(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> this.isStabilized(callbackContext, POST_CHECK_UPDATE) &&
                        database.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> updateDatabaseParameters(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val database = getDatabase(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Database::UpdateParameters...");
        return proxy
                .initiate("AWS-Lightsail-Database::UpdateParameters", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToUpdateParametersRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> database.updateParameters(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> this.isStabilized(callbackContext, POST_CHECK_UPDATE_PARAMS) &&
                        database.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }
}
