package software.amazon.lightsail.distribution;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetDistributionsRequest;
import software.amazon.awssdk.services.lightsail.model.GetDistributionsResponse;
import software.amazon.cloudformation.proxy.*;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                       final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                       final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        final AwsRequest awsRequest = Translator.translateToListRequest(request.getNextToken());

        GetDistributionsResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2((GetDistributionsRequest) awsRequest,
                proxyClient.client()::getDistributions);

        String nextToken = awsResponse.nextPageToken();
        nextToken = (nextToken == null || nextToken.length() == 0) ? null : nextToken;

        return ProgressEvent.<ResourceModel, CallbackContext> builder()
                .resourceModels(Translator.translateFromListRequest(awsResponse)).nextToken(nextToken)
                .status(OperationStatus.SUCCESS).build();
    }
}
