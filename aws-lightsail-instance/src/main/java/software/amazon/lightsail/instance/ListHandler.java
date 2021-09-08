package software.amazon.lightsail.instance;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetInstancesRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstancesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * List Handler will list all the Lightsail Instances along with the next page token.
 */
public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
            final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        // STEP 1 [construct a body of a request]
        final AwsRequest awsRequest = Translator.translateToListRequest(request.getNextToken());

        // STEP 2 [make an api call]
        GetInstancesResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2((GetInstancesRequest) awsRequest,
                proxyClient.client()::getInstances);

        // STEP 3 [get next page token]
        String nextToken = awsResponse.nextPageToken();

        return ProgressEvent.<ResourceModel, CallbackContext> builder()
                .resourceModels(Translator.translateFromListRequest(awsResponse)).nextToken(nextToken)
                .status(OperationStatus.SUCCESS).build();
    }
}
