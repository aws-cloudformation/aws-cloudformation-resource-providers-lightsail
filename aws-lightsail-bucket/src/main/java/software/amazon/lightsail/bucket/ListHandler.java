package software.amazon.lightsail.bucket;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetBucketsRequest;
import software.amazon.awssdk.services.lightsail.model.GetBucketsResponse;
import software.amazon.cloudformation.proxy.*;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                       final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                       final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        final AwsRequest awsRequest = Translator.translateToListRequest(request.getNextToken());

        GetBucketsResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2((GetBucketsRequest) awsRequest,
                proxyClient.client()::getBuckets);

        String nextToken = awsResponse.nextPageToken();
        nextToken = (nextToken == null || nextToken.length() == 0) ? null : nextToken;

        return ProgressEvent.<ResourceModel, CallbackContext> builder()
                .resourceModels(Translator.translateFromListRequest(awsResponse)).nextToken(nextToken)
                .status(OperationStatus.SUCCESS).build();
    }
}
