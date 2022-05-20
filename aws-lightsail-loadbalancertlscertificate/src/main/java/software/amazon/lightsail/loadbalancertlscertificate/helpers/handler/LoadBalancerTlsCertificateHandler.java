package software.amazon.lightsail.loadbalancertlscertificate.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.ResourceType;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.loadbalancertlscertificate.CallbackContext;
import software.amazon.lightsail.loadbalancertlscertificate.ResourceModel;
import software.amazon.lightsail.loadbalancertlscertificate.Translator;
import software.amazon.lightsail.loadbalancertlscertificate.helpers.resource.LoadBalancerTlsCertificate;

import static software.amazon.lightsail.loadbalancertlscertificate.BaseHandlerStd.*;
import static software.amazon.lightsail.loadbalancertlscertificate.CallbackContext.*;

@RequiredArgsConstructor
public class LoadBalancerTlsCertificateHandler extends ResourceHandler {

    final AmazonWebServicesClientProxy proxy;
    final CallbackContext callbackContext;
    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    protected LoadBalancerTlsCertificate getLoadBalancerTlsCertificate(final ResourceHandlerRequest<ResourceModel> request,
                                                                       final ProxyClient<LightsailClient> proxyClient, final Logger logger) {
        return new LoadBalancerTlsCertificate(request.getDesiredResourceState(), logger, proxyClient, request);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancerTlsCertificate = getLoadBalancerTlsCertificate(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancerTlsCertificate::Update::PreCheck...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancerTlsCertificate::Update::PreCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> loadBalancerTlsCertificate.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> update(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        return attachCertificate(progress).then(this::modifyHttpsRedirectionAttribute);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> attachCertificate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancerTlsCertificate = getLoadBalancerTlsCertificate(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancerTlsCertificate::Update::AttachCertificate...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancerTlsCertificate::Update::AttachCertificate", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> loadBalancerTlsCertificate.attachToLoadBalancer())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> modifyHttpsRedirectionAttribute(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancerTlsCertificate = getLoadBalancerTlsCertificate(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancerTlsCertificate::Update::ModifyHttpsRedirectionAttribute...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancerTlsCertificate::Update::ModifyHttpsRedirectionAttribute", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> loadBalancerTlsCertificate.modifyHttpsRedirection())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancerTlsCertificate = getLoadBalancerTlsCertificate(resourceModelRequest, proxyClient, logger);
        if (callbackContext.getIsPreCheckDone(PRE_CHECK_CREATE)) {
            return progress;
        }
        logger.log("Executing AWS-Lightsail-LoadBalancerTlsCertificate::Create::PreExistenceCheck...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancerTlsCertificate::Create::PreExistenceCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest).makeServiceCall((awsRequest, client) -> {
                    loadBalancerTlsCertificate.read(awsRequest);
                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    throw new CfnAlreadyExistsException(ResourceType.LOAD_BALANCER_TLS_CERTIFICATE.toString(),
                            resourceModel.getCertificateName());
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
        val loadBalancerTlsCertificate = getLoadBalancerTlsCertificate(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancerTlsCertificate::Create...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancerTlsCertificate::Create", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest).backoffDelay(BACKOFF_DELAY)
                .makeServiceCall((awsRequest, client) -> loadBalancerTlsCertificate.create(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> (this.isStabilized(callbackContext, POST_CHECK_CREATE) &&
                        loadBalancerTlsCertificate.isStabilizedCreate()))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> preDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancerTlsCertificate = getLoadBalancerTlsCertificate(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancerTlsCertificate::Delete::PreDeletionCheck..");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancerTlsCertificate::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> loadBalancerTlsCertificate.read(awsRequest))
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val loadBalancerTlsCertificate = getLoadBalancerTlsCertificate(resourceModelRequest, proxyClient, logger);
        logger.log("Executing AWS-Lightsail-LoadBalancerTlsCertificate::Delete...");
        return proxy
                .initiate("AWS-Lightsail-LoadBalancerTlsCertificate::Delete", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> loadBalancerTlsCertificate.delete(awsRequest))
                .stabilize((awsRequest, awsResponse, client, model, context) -> loadBalancerTlsCertificate.isStabilizedDelete())
                .handleError((awsRequest, exception, client, model, context) -> handleError(exception, model,
                        callbackContext, ImmutableList.of(), logger, this.getClass().getSimpleName()))
                .progress();
    }

}
