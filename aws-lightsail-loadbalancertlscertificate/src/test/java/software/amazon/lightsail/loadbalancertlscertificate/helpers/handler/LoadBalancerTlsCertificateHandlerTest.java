package software.amazon.lightsail.loadbalancertlscertificate.helpers.handler;

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
import software.amazon.lightsail.loadbalancertlscertificate.*;
import software.amazon.lightsail.loadbalancertlscertificate.helpers.resource.LoadBalancerTlsCertificate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.loadbalancertlscertificate.AbstractTestBase.MOCK_CREDENTIALS;
import static software.amazon.lightsail.loadbalancertlscertificate.CallbackContext.POST_CHECK_CREATE;
import static software.amazon.lightsail.loadbalancertlscertificate.CallbackContext.PRE_CHECK_CREATE;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerTlsCertificateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    @Mock
    private LoadBalancerTlsCertificate loadBalancerTlsCertificate;

    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        loadBalancerTlsCertificate = mock(LoadBalancerTlsCertificate.class);
        logger = mock(Logger.class);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testPreCreate_resourceExists() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().certificateName("testCert").build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        val testLoadBalancerTlsCertificateHandler =
                spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger, proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.read(any()))
                .thenReturn(GetLoadBalancerTlsCertificatesResponse.builder()
                        .tlsCertificates(software.amazon.awssdk.services.lightsail.model.LoadBalancerTlsCertificate.builder().name("testCert").build()).build());

        try {
            testLoadBalancerTlsCertificateHandler.preCreate(ProgressEvent.progress(model, callbackContext));
            fail();
        } catch (CfnAlreadyExistsException ex) {
            // This exception is expected.
        }

        verify(loadBalancerTlsCertificate, times(1)).read(any());
    }

    @Test
    public void testPreCreate_resourceDoesNotExist() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.preCreate(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, times(1)).read(any());
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

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        callbackContext.getIsPreCheckDone().put(PRE_CHECK_CREATE, true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.preCreate(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, never()).read(any());
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

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.create(any()))
                .thenReturn(CreateLoadBalancerTlsCertificateResponse.builder().build());
        when(loadBalancerTlsCertificate.isStabilizedCreate())
                .thenReturn(true);
        callbackContext.incrementWaitCount(POST_CHECK_CREATE);

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, times(1)).create(any());
        verify(loadBalancerTlsCertificate, times(1)).isStabilizedCreate();
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

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("ServiceException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, times(1)).create(any());
        verify(loadBalancerTlsCertificate, never()).isStabilizedCreate();
        assertThat(loadBalancerTlsCertificate).isNotNull();
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

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("ThrottlingException").build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, times(1)).create(any());
        verify(loadBalancerTlsCertificate, never()).isStabilizedCreate();
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

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDeniedException").build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, times(1)).create(any());
        verify(loadBalancerTlsCertificate, never()).isStabilizedCreate();
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

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.create(any()))
                .thenThrow(RuntimeException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, times(1)).create(any());
        verify(loadBalancerTlsCertificate, never()).isStabilizedCreate();
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

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.read(any()))
                .thenReturn(GetLoadBalancerTlsCertificatesResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.preUpdate(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, times(1)).read(any());
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

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.preUpdate(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, times(1)).read(any());
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

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.attachToLoadBalancer())
                .thenReturn(null);

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.update(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, times(1)).attachToLoadBalancer();
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

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.read(any()))
                .thenReturn(GetLoadBalancerResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.preDelete(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, times(1)).read(any());
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

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.preDelete(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, times(1)).read(any());
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

        val testLoadBalancerTlsCertificateHandler = spy(new LoadBalancerTlsCertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(loadBalancerTlsCertificate)
                .when(testLoadBalancerTlsCertificateHandler).getLoadBalancerTlsCertificate(any(), any(), any());

        when(loadBalancerTlsCertificate.delete(any()))
                .thenReturn(DeleteLoadBalancerTlsCertificateResponse.builder().build());
        when(loadBalancerTlsCertificate.isStabilizedDelete())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testLoadBalancerTlsCertificateHandler.delete(ProgressEvent.progress(model, callbackContext));

        verify(loadBalancerTlsCertificate, times(1)).delete(any());
        verify(loadBalancerTlsCertificate, times(1)).isStabilizedDelete();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

}
