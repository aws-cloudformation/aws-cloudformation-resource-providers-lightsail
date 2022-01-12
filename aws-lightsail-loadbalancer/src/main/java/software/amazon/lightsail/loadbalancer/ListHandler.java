package software.amazon.lightsail.loadbalancer;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetLoadBalancersRequest;
import software.amazon.awssdk.services.lightsail.model.GetLoadBalancersResponse;
import software.amazon.cloudformation.proxy.*;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                       final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                       final ProxyClient<LightsailClient> proxyClient, final Logger logger) {

        final AwsRequest awsRequest = Translator.translateToListRequest(request.getNextToken());

        GetLoadBalancersResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2((GetLoadBalancersRequest) awsRequest,
                proxyClient.client()::getLoadBalancers);

        String nextToken = awsResponse.nextPageToken();

        return ProgressEvent.<ResourceModel, CallbackContext> builder()
                .resourceModels(Translator.translateFromListRequest(awsResponse)).nextToken(nextToken)
                .status(OperationStatus.SUCCESS).build();
    }
}
