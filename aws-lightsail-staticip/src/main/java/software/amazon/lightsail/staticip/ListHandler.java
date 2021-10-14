package software.amazon.lightsail.staticip;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetStaticIpsRequest;
import software.amazon.awssdk.services.lightsail.model.GetStaticIpsResponse;
import software.amazon.cloudformation.proxy.*;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
           final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
           final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        final AwsRequest awsRequest = Translator.translateToListRequest(request.getNextToken());

        GetStaticIpsResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2((GetStaticIpsRequest) awsRequest,
                proxyClient.client()::getStaticIps);

        String nextToken = awsResponse.nextPageToken();

        return ProgressEvent.<ResourceModel, CallbackContext> builder()
                .resourceModels(Translator.translateFromListRequest(awsResponse)).nextToken(nextToken)
                .status(OperationStatus.SUCCESS).build();
    }
}
