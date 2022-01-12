package software.amazon.lightsail.loadbalancertlscertificate;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetLoadBalancerTlsCertificatesRequest;
import software.amazon.awssdk.services.lightsail.model.GetLoadBalancerTlsCertificatesResponse;
import software.amazon.cloudformation.proxy.*;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                       final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                       final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        final AwsRequest awsRequest = Translator.translateToListRequest(request.getDesiredResourceState());

        GetLoadBalancerTlsCertificatesResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2((GetLoadBalancerTlsCertificatesRequest) awsRequest,
                proxyClient.client()::getLoadBalancerTlsCertificates);

        return ProgressEvent.<ResourceModel, CallbackContext> builder()
                .resourceModels(Translator.translateFromListRequest(awsResponse))
                .status(OperationStatus.SUCCESS).build();
    }
}
