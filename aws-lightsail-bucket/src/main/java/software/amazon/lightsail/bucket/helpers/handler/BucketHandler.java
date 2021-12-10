package software.amazon.lightsail.bucket.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetBucketsRequest;
import software.amazon.awssdk.services.lightsail.model.GetBucketsResponse;
import software.amazon.awssdk.services.lightsail.model.ResourceType;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.bucket.CallbackContext;
import software.amazon.lightsail.bucket.ResourceModel;
import software.amazon.lightsail.bucket.Translator;
import software.amazon.lightsail.bucket.helpers.resource.Bucket;
import software.amazon.lightsail.bucket.helpers.resource.Instance;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static software.amazon.lightsail.bucket.BaseHandlerStd.*;
import static software.amazon.lightsail.bucket.CallbackContext.*;

@RequiredArgsConstructor
public class BucketHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    protected Bucket getBucket(final ResourceHandlerRequest<ResourceModel> request,
                                   final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Bucket(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    protected Instance getInstance(final ResourceHandlerRequest<ResourceModel> request,
                               final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new Instance(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val bucket = getBucket(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Bucket::Update::PreCheck...");
        return proxy
                .initiate("AWS-Lightsail-Bucket::Update::PreCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> bucket.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model,
                            context) -> this.isStabilized(this.callbackContext, PRE_CHECK_UPDATE)
                        || bucket.isStabilizedCreate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {

        return updateBucket(progress).then(this::updateBucketBundle).then(this::preDetachInstances)
                .then(this::detachInstances).then(this::preAttachInstances).then(this::attachInstances);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val bucket = getBucket(resourceModelRequest, proxyClient, logger);
        if (callbackContext.getIsPreCheckDone(PRE_CHECK_CREATE)) {
            return progress;
        }
        logger.log("Executing AWS-Lightsail-Bucket::Create::PreExistenceCheck...");
        return proxy
                .initiate("AWS-Lightsail-Bucket::Create::PreExistenceCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).makeServiceCall((awsRequest, client) -> {
                    bucket.read(awsRequest);
                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    throw new CfnAlreadyExistsException(ResourceType.BUCKET.toString(),
                            ((GetBucketsRequest) awsRequest).bucketName());
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
        val bucket = getBucket(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Bucket::Create...");
        return proxy
                .initiate("AWS-Lightsail-Bucket::Create", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> bucket.create(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> (this.isStabilized(callbackContext, POST_CHECK_CREATE) &&
                        bucket.isStabilizedCreate()))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val bucket = getBucket(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Bucket::Delete::PreDeletionCheck..");
        return proxy
                .initiate("AWS-Lightsail-Bucket::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> bucket.read(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> this.isStabilized(this.callbackContext, PRE_CHECK_DELETE) ||
                        bucket.isStabilizedUpdate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val bucket = getBucket(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Bucket::Delete...");
        return proxy
                .initiate("AWS-Lightsail-Bucket::Delete", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> bucket.delete(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> bucket.isStabilizedDelete())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> updateBucket(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val bucket = getBucket(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Bucket::Update...");
        return proxy
                .initiate("AWS-Lightsail-Bucket::Update", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToUpdateRequest)
                .makeServiceCall((awsRequest, client) -> bucket.update(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> bucket.isStabilizedCreate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> updateBucketBundle(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val bucket = getBucket(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Bucket::Update::Bundle...");
        return proxy
                .initiate("AWS-Lightsail-Bucket::Update::Bundle", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToUpdateBundleRequest)
                .makeServiceCall((awsRequest, client) -> bucket.updateBundle(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> bucket.isStabilizedCreate())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> preDetachInstances(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val bucket = getBucket(resourceModelRequest, proxyClient, logger);
        val instance = getInstance(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Bucket::Update::PreDetachInstances...");
        return proxy
                .initiate("AWS-Lightsail-Bucket::Update::PreDetachInstances", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> {
                    // Just return empty Get Response. We are not making any service call here.
                    // We are making sure in the stabilize step that all instances that need to be detached are running.
                    return GetBucketsResponse.builder().build();
                })
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    Set<String> desiredResources = resourceModelRequest.getDesiredResourceState().getResourcesReceivingAccess();
                    Set<String> currentResources = bucket.getCurrentResourceModelFromLightsail().getResourcesReceivingAccess();

                    Set<String> resourcesToRemove = bucket.setDifference(currentResources, desiredResources);

                    for (val resource: resourcesToRemove) {
                        if (!instance.isStabilized(resource)) {
                            return this.isStabilized(callbackContext, PRE_CHECK_DETACH);
                        }
                    }
                    return true;
                })
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> detachInstances(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val bucket = getBucket(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Bucket::Update::DetachInstances...");
        return proxy
                .initiate("AWS-Lightsail-Bucket::Update::DetachInstances", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> bucket.detachInstances(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> (this.isStabilized(callbackContext, POST_CHECK_DETACH) &&
                        bucket.isStabilizedCreate()))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> preAttachInstances(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val bucket = getBucket(resourceModelRequest, proxyClient, logger);
        val instance = getInstance(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Bucket::Update::PreAttachInstances...");
        return proxy
                .initiate("AWS-Lightsail-Bucket::Update::PreAttachInstances", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> {
                    // Just return empty Get Response. We are not making any service call here.
                    // Making sure that all instances that need to be attached are not attached to other buckets.
                    return GetBucketsResponse.builder().build();
                })
                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                    Set<String> desiredResources = resourceModelRequest.getDesiredResourceState().getResourcesReceivingAccess();
                    Set<String> currentResources = bucket.getCurrentResourceModelFromLightsail().getResourcesReceivingAccess();

                    Set<String> resourcesToAdd = bucket.setDifference(desiredResources, currentResources);

                    val getBucketsResponse = (GetBucketsResponse) bucket.readAll(awsRequest);
                    Set<String> alreadyAttached = new HashSet<>();
                    for (val buck: getBucketsResponse.buckets()) {
                        alreadyAttached.addAll(buck.resourcesReceivingAccess().stream()
                                .map(resourceReceivingAccess -> resourceReceivingAccess.name()).collect(Collectors.toSet()));
                    }
                    logger.log("Already attached resources: " + alreadyAttached.toString());
                    for (val resource: resourcesToAdd) {
                        if (!instance.isStabilized(resource) || alreadyAttached.contains(resource)) {
                            return this.isStabilized(callbackContext, PRE_CHECK_ATTACH);
                        }
                    }
                    return true;
                })
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> attachInstances(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val bucket = getBucket(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-Bucket::Update::AttachInstances...");
        return proxy
                .initiate("AWS-Lightsail-Bucket::Update::AttachInstances", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> bucket.attachInstances(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> (this.isStabilized(callbackContext, POST_CHECK_ATTACH) &&
                        bucket.isStabilizedCreate()))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

}
