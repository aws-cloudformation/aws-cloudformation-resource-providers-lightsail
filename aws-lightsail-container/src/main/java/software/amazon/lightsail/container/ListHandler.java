package software.amazon.lightsail.container;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetContainerServicesRequest;
import software.amazon.awssdk.services.lightsail.model.GetContainerServicesResponse;
import software.amazon.cloudformation.proxy.*;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                       final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                       final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        final AwsRequest awsRequest = Translator.translateToListRequest();

        GetContainerServicesResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2((GetContainerServicesRequest) awsRequest,
                proxyClient.client()::getContainerServices);


        return ProgressEvent.<ResourceModel, CallbackContext> builder()
                .resourceModels(Translator.translateFromListRequest(awsResponse))
                .status(OperationStatus.SUCCESS).build();
    }
}