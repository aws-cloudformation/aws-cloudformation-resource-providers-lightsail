package software.amazon.lightsail.certificate;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetCertificatesRequest;
import software.amazon.awssdk.services.lightsail.model.GetCertificatesResponse;
import software.amazon.cloudformation.proxy.*;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                       final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                       final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        final AwsRequest awsRequest = Translator.translateToListRequest();

        GetCertificatesResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2((GetCertificatesRequest) awsRequest,
                proxyClient.client()::getCertificates);

        return ProgressEvent.<ResourceModel, CallbackContext> builder()
                .resourceModels(Translator.translateFromListRequest(awsResponse))
                .status(OperationStatus.SUCCESS).build();
    }
}
