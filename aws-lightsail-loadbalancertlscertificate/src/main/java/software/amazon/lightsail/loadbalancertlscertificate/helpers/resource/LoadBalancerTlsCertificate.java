package software.amazon.lightsail.loadbalancertlscertificate.helpers.resource;

import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.loadbalancertlscertificate.ResourceModel;
import software.amazon.lightsail.loadbalancertlscertificate.helpers.GetModifiedLbTlsCertResponse;

/**
 * Helper class to handle LoadBalancer operations.
 */
@RequiredArgsConstructor
public class LoadBalancerTlsCertificate implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    public AwsResponse update(AwsRequest request) {
        AwsResponse awsResponse = null;

        return awsResponse;
    }

    @Override
    public AwsResponse create(AwsRequest request) {
        logger.log(String.format("Validating Cfn template for LoadBalancerTlsCertificate: %s", resourceModel.getCertificateName()));
        validateTemplate(resourceModel);
        logger.log(String.format("Creating LoadBalancerTlsCertificate: %s", resourceModel.getCertificateName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((CreateLoadBalancerTlsCertificateRequest) request),
                proxyClient.client()::createLoadBalancerTlsCertificate);
        logger.log(String.format("Successfully created LoadBalancerTlsCertificate: %s", resourceModel.getCertificateName()));
        return awsResponse;
    }

    @Override
    public AwsResponse delete(AwsRequest request) {
        logger.log(String.format("Deleting LoadBalancerTlsCertificate: %s", resourceModel.getCertificateName()));
        AwsResponse awsResponse = null;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((DeleteLoadBalancerTlsCertificateRequest) request),
                proxyClient.client()::deleteLoadBalancerTlsCertificate);
        logger.log(String.format("Successfully deleted LoadBalancerTlsCertificate: %s", resourceModel.getCertificateName()));
        return awsResponse;
    }

    /**
     * Read LoadBalancer.
     *
     * @param request
     *
     * @return GetModifiedLbTlsCertResponse
     */
    @Override
    public GetModifiedLbTlsCertResponse read(AwsRequest request) {
        val loadBalancerName = resourceModel.getLoadBalancerName();
        logger.log(String.format("Reading certificates for LoadBalancer: %s", loadBalancerName));
        val response = proxyClient.injectCredentialsAndInvokeV2(GetLoadBalancerTlsCertificatesRequest.builder()
                .loadBalancerName(loadBalancerName).build(), proxyClient.client()::getLoadBalancerTlsCertificates);

        for (software.amazon.awssdk.services.lightsail.model.LoadBalancerTlsCertificate cert: response.tlsCertificates()) {
            if (cert.name().equals(resourceModel.getCertificateName())) {
                val lbResponse = proxyClient.injectCredentialsAndInvokeV2(GetLoadBalancerRequest.builder()
                        .loadBalancerName(loadBalancerName).build(), proxyClient.client()::getLoadBalancer);
                return new GetModifiedLbTlsCertResponse(response, lbResponse.loadBalancer().httpsRedirectionEnabled());
            }
        }
        throw NotFoundException.builder().code("NotFoundException").message("The LoadBalancerTlsCert does not exist").statusCode(400)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NotFoundException").errorMessage("The LoadBalancerTlsCert does not exist")
                        .serviceName("Lightsail").build()).build();
    }

    @Override
    public boolean isStabilizedDelete() {
        final boolean stabilized = false;
        logger.log(String.format("Checking if LoadBalancerTlsCertificate: %s deletion has stabilized.",
                resourceModel.getLoadBalancerName(), stabilized));
        try {
            this.read(GetLoadBalancerTlsCertificatesRequest.builder().loadBalancerName(resourceModel.getLoadBalancerName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            logger.log(String.format("LoadBalancerTlsCertificate: %s deletion has stabilized", resourceModel.getCertificateName()));
            return true;
        }
        return stabilized;
    }

    public boolean isStabilizedCreate() {
        final boolean stabilized = true;
        logger.log(String.format("Checking if LoadBalancerTlsCertificate: %s creation has stabilized.",
                resourceModel.getLoadBalancerName(), stabilized));
        try {
            this.read(GetLoadBalancerTlsCertificatesRequest.builder().loadBalancerName(resourceModel.getLoadBalancerName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            return false;
        }
        logger.log(String.format("LoadBalancerTlsCertificate: %s creation has stabilized", resourceModel.getCertificateName()));
        return stabilized;
    }

    public AwsResponse attachToLoadBalancer() {
        AwsResponse awsResponse = null;
        validateTemplate(resourceModelRequest.getDesiredResourceState());
        Boolean isAttachRequired = resourceModelRequest.getDesiredResourceState().getIsAttached();
        if (isAttachRequired == null || !isAttachRequired) {
            logger.log(String.format("Attach not required for LoadBalancerTlsCertificate: %s.", resourceModel.getCertificateName()));
            return awsResponse;
        }
        return proxyClient.injectCredentialsAndInvokeV2(AttachLoadBalancerTlsCertificateRequest.builder()
                        .loadBalancerName(resourceModel.getLoadBalancerName()).certificateName(resourceModel.getCertificateName()).build(),
                proxyClient.client()::attachLoadBalancerTlsCertificate);
    }

    public AwsResponse modifyHttpsRedirection() {
        AwsResponse awsResponse = null;
        Boolean isAttachRequired = resourceModelRequest.getDesiredResourceState().getIsAttached();
        if (isAttachRequired == null || !isAttachRequired) {
            logger.log(String.format("HttpsRedirection modification not needed. LoadBalancerTlsCertificate: %s is not attached.", resourceModel.getCertificateName()));
            return awsResponse;
        }
        Boolean desiredState = resourceModelRequest.getDesiredResourceState().getHttpsRedirectionEnabled();
        Boolean httpsRedirection = desiredState == null ? false : desiredState;
        logger.log(String.format("Modifying HttpsRedirect for LoadBalancer: %s to %s.", resourceModel.getLoadBalancerName(), httpsRedirection));
        return proxyClient.injectCredentialsAndInvokeV2(UpdateLoadBalancerAttributeRequest.builder()
                        .loadBalancerName(resourceModel.getLoadBalancerName()).attributeName("HttpsRedirectionEnabled")
                        .attributeValue(String.valueOf(httpsRedirection)).build(),
                proxyClient.client()::updateLoadBalancerAttribute);
    }

    /**
     * HttpsRedirectionEnabled parameter can only be set when a valid LoadBalancerTlsCertificate is attached to the LoadBalancer.
     * We have set a constraint on the template that HttpsRedirectionEnabled can only be used in a certificate that is
     * attached to the LoadBalancer. This function verifies that.
     */
    public void validateTemplate(ResourceModel model) {
        boolean isAttached = model.getIsAttached() == null ? false : model.getIsAttached();
        if (isAttached !=  true && model.getHttpsRedirectionEnabled() != null) {
            throw new CfnInvalidRequestException("HttpsRedirectionEnabled field can only be set from a LoadBalancerTlsCertificate that is attached.");
        }
    }

    @Override
    public boolean isStabilizedUpdate() {
        return false;
    }

    @Override
    public boolean isSafeExceptionCreateOrUpdate(Exception e) {
        return false;
    }

    @Override
    public boolean isSafeExceptionDelete(Exception e) {
        if (e instanceof CfnNotFoundException || e instanceof NotFoundException) {
            return true; // Its stabilized if the resource is gone..
        }
        return false;
    }

}
