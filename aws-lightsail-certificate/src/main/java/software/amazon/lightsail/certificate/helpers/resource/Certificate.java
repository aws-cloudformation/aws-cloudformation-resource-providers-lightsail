package software.amazon.lightsail.certificate.helpers.resource;

import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.certificate.ResourceModel;

/**
 * Helper class to handle Certificate operations.
 */
@RequiredArgsConstructor
public class Certificate implements ResourceHelper {

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
        logger.log(String.format("Creating Certificate: %s", resourceModel.getCertificateName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((CreateCertificateRequest) request),
                proxyClient.client()::createCertificate);
        logger.log(String.format("Successfully created Certificate: %s", resourceModel.getCertificateName()));
        return awsResponse;
    }

    @Override
    public AwsResponse delete(AwsRequest request) {
        logger.log(String.format("Deleting Certificate: %s", resourceModel.getCertificateName()));
        AwsResponse awsResponse = null;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((DeleteCertificateRequest) request),
                proxyClient.client()::deleteCertificate);
        logger.log(String.format("Successfully deleted Certificate: %s", resourceModel.getCertificateName()));
        return awsResponse;
    }

    /**
     * Read Certificate.
     *
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse read(AwsRequest request) {
        val certificateName = ((GetCertificatesRequest) request).certificateName();
        logger.log(String.format("Reading Certificate: %s", certificateName));
        val response = proxyClient.injectCredentialsAndInvokeV2(GetCertificatesRequest.builder()
                .certificateName(certificateName).build(), proxyClient.client()::getCertificates);

        if (response.certificates().size() > 0) {
            return response;
        }
        throw NotFoundException.builder().code("NotFoundException").message("The Certificate does not exist").statusCode(400)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NotFoundException").errorMessage("The Certificate does not exist")
                        .serviceName("Lightsail").build()).build();
    }

    public boolean isStabilizedCreate() {
        final boolean stabilized = true;
        logger.log(String.format("Checking if Certificate: %s creation has stabilized.",
                resourceModel.getCertificateName(), stabilized));
        try {
            this.read(GetCertificatesRequest.builder().certificateName(resourceModel.getCertificateName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            return false;
        }
        logger.log(String.format("Certificate: %s creation has stabilized", resourceModel.getCertificateName()));
        return stabilized;
    }

    @Override
    public boolean isStabilizedDelete() {
        final boolean stabilized = false;
        logger.log(String.format("Checking if Certificate: %s deletion has stabilized.",
                resourceModel.getCertificateName(), stabilized));
        try {
            this.read(GetCertificatesRequest.builder().certificateName(resourceModel.getCertificateName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            logger.log(String.format("Certificate: %s deletion has stabilized", resourceModel.getCertificateName()));
            return true;
        }
        return stabilized;
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
