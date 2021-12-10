package software.amazon.lightsail.loadbalancer.helpers.handler;

import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.loadbalancer.*;
import software.amazon.lightsail.loadbalancer.helpers.resource.Instance;
import software.amazon.lightsail.loadbalancer.helpers.resource.LoadBalancer;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.loadbalancer.AbstractTestBase.MOCK_CREDENTIALS;
import static software.amazon.lightsail.loadbalancer.CallbackContext.*;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    @Mock
    private LoadBalancer loadBalancer;

    @Mock
    private software.amazon.lightsail.loadbalancer.helpers.resource.Instance instance;

    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        loadBalancer = mock(LoadBalancer.class);
        instance = mock(Instance.class);
        logger = mock(Logger.class);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testPreCreate_resourceExists() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.read(any()))
                .thenReturn(GetLoadBalancerResponse.builder().build());

        try {
            testLoadBalancerHandler.preCreate(ProgressEvent.progress(model, callbackContext));
            fail();
        } catch (CfnAlreadyExistsException ex) {
            // This exception is expected.
        }

        verify(loadBalancer, times(1)).read(any());
    }

    @Test
    public void testPreCreate_resourceDoesNotExist() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.preCreate(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).read(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testPreCreate_preCheckDone() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        callbackContext.getIsPreCheckDone().put(PRE_CHECK_CREATE, true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.preCreate(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, never()).read(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testCreate() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.create(any()))
                .thenReturn(CreateLoadBalancerResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).create(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testCreate_error() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("ServiceException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).create(any());
        assertThat(loadBalancer).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void testCreate_errorThrottling() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("ThrottlingException").build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).create(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void testCreate_errorAuth() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDeniedException").build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).create(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }

    @Test
    public void testCreate_errorRandom() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.create(any()))
                .thenThrow(RuntimeException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).create(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void testPreUpdate() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.read(any()))
                .thenReturn(GetLoadBalancerResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.preUpdate(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).read(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testPreUpdate_doesNotExist() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.preUpdate(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).read(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void testUpdate() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testLoadBalancerHandler).updateLoadBalancerAttributes(any());
        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testLoadBalancerHandler).detachInstances(any());
        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testLoadBalancerHandler).preAttachInstances(any());
        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testLoadBalancerHandler).attachInstances(any());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.update(ProgressEvent.progress(model, callbackContext));

        verify(testLoadBalancerHandler, times(1)).updateLoadBalancerAttributes(any());
        verify(testLoadBalancerHandler, times(1)).detachInstances(any());
        verify(testLoadBalancerHandler, times(1)).preAttachInstances(any());
        verify(testLoadBalancerHandler, times(1)).attachInstances(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testPreDelete() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.read(any()))
                .thenReturn(GetLoadBalancerResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.preDelete(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).read(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testPreDelete_doesNotExist() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.preDelete(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).read(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void testDelete() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.delete(any()))
                .thenReturn(DeleteLoadBalancerResponse.builder().build());
        when(loadBalancer.isStabilizedDelete())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.delete(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).delete(any());
        verify(loadBalancer, times(1)).isStabilizedDelete();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testUpdateLoadBalancerAttributes() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.updateAttributes(any()))
                .thenReturn(null);

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.updateLoadBalancerAttributes(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).updateAttributes(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testDetachInstances() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.detachInstances(any()))
                .thenReturn(DetachInstancesFromLoadBalancerResponse.builder().build());
        when(loadBalancer.isStabilizedInstances())
                .thenReturn(true);
        callbackContext.incrementWaitCount(POST_DETACH_WAIT);

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.detachInstances(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).detachInstances(any());
        verify(loadBalancer, times(1)).isStabilizedInstances();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testPreAttachInstances() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());
        doReturn(instance)
                .when(testLoadBalancerHandler).getInstance(any(), any(), any());

        when(loadBalancer.getCurrentResourceModelFromLightsail())
                .thenReturn(ResourceModel.builder().build());
        when(loadBalancer.setDifference(any(), any()))
                .thenReturn(new HashSet<>(Arrays.asList("resource1", "resource2")));
        when(instance.isStabilized(any()))
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.preAttachInstances(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).getCurrentResourceModelFromLightsail();
        verify(loadBalancer, times(1)).setDifference(any(), any());
        verify(instance, times(2)).isStabilized(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testAttachInstances() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerHandler = spy(new LoadBalancerHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancer)
                .when(testLoadBalancerHandler).getLoadBalancer(any(), any(), any());

        when(loadBalancer.attachInstances(any()))
                .thenReturn(AttachInstancesToLoadBalancerResponse.builder().build());
        when(loadBalancer.isStabilizedInstances())
                .thenReturn(true);
        callbackContext.incrementWaitCount(POST_ATTACH_WAIT);

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerHandler.attachInstances(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancer, times(1)).attachInstances(any());
        verify(loadBalancer, times(1)).isStabilizedInstances();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

}
