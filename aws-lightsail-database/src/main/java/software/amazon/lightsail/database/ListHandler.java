package software.amazon.lightsail.database;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetRelationalDatabasesRequest;
import software.amazon.awssdk.services.lightsail.model.GetRelationalDatabasesResponse;
import software.amazon.cloudformation.proxy.*;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                       final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                       final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        final AwsRequest awsRequest = Translator.translateToListRequest(request.getNextToken());

        GetRelationalDatabasesResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2((GetRelationalDatabasesRequest) awsRequest,
                proxyClient.client()::getRelationalDatabases);

        String nextToken = awsResponse.nextPageToken();

        return ProgressEvent.<ResourceModel, CallbackContext> builder()
                .resourceModels(Translator.translateFromListRequest(awsResponse)).nextToken(nextToken)
                .status(OperationStatus.SUCCESS).build();
    }
}
