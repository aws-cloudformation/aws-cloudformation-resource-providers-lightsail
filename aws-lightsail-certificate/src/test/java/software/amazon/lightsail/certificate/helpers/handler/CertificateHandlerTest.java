package software.amazon.lightsail.certificate.helpers.handler;

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
import software.amazon.lightsail.certificate.*;
import software.amazon.lightsail.certificate.helpers.resource.Certificate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.certificate.AbstractTestBase.MOCK_CREDENTIALS;
import static software.amazon.lightsail.certificate.CallbackContext.*;

@ExtendWith(MockitoExtension.class)
public class CertificateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    @Mock
    private software.amazon.lightsail.certificate.helpers.resource.Certificate certificate;

    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        certificate = mock(Certificate.class);
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

        val testCertificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testCertificateHandler).getCertificate(any(), any(), any());

        when(certificate.read(any(GetCertificatesRequest.class)))
                .thenReturn(GetCertificatesResponse.builder().build());

        try {
            testCertificateHandler.preCreate(ProgressEvent.progress(model, callbackContext));
            fail();
        } catch (CfnAlreadyExistsException ex) {
            // This exception is expected.
        }

        verify(certificate, times(1)).read(any());
    }

    @Test
    public void testPreCreate_resourceDoesNotExist() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testCertificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testCertificateHandler).getCertificate(any(), any(), any());

        when(certificate.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testCertificateHandler.preCreate(ProgressEvent.progress(model, callbackContext));

        verify(certificate, times(1)).read(any());
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

        val testCertificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testCertificateHandler).getCertificate(any(), any(), any());

        callbackContext.getIsPreCheckDone().put(PRE_CHECK_CREATE, true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testCertificateHandler.preCreate(ProgressEvent.progress(model, callbackContext));

        verify(certificate, never()).read(any());
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

        val testCertificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testCertificateHandler).getCertificate(any(), any(), any());

        when(certificate.create(any()))
                .thenReturn(CreateCertificateResponse.builder().build());
        when(certificate.isStabilizedCreate())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testCertificateHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(certificate, times(1)).create(any());
        verify(certificate, times(1)).isStabilizedCreate();
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

        val testCertificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testCertificateHandler).getCertificate(any(), any(), any());

        when(certificate.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("ServiceException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testCertificateHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(certificate, times(1)).create(any());
        assertThat(certificate).isNotNull();
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

        val testCertificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testCertificateHandler).getCertificate(any(), any(), any());

        when(certificate.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("ThrottlingException").build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testCertificateHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(certificate, times(1)).create(any());
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

        val testCertificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testCertificateHandler).getCertificate(any(), any(), any());

        when(certificate.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDeniedException").build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testCertificateHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(certificate, times(1)).create(any());
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

        val testCertificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testCertificateHandler).getCertificate(any(), any(), any());

        when(certificate.create(any()))
                .thenThrow(RuntimeException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = testCertificateHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(certificate, times(1)).create(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void testPreDelete() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testCertificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testCertificateHandler).getCertificate(any(), any(), any());

        when(certificate.read(any()))
                .thenReturn(GetCertificatesResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testCertificateHandler.preDelete(ProgressEvent.progress(model, callbackContext));

        verify(certificate, times(1)).read(any());
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

        val testCertificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testCertificateHandler).getCertificate(any(), any(), any());

        when(certificate.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testCertificateHandler.preDelete(ProgressEvent.progress(model, callbackContext));

        verify(certificate, times(1)).read(any());
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

        val testCertificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testCertificateHandler).getCertificate(any(), any(), any());

        when(certificate.delete(any()))
                .thenReturn(DeleteCertificateResponse.builder().build());
        when(certificate.isStabilizedDelete())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testCertificateHandler.delete(ProgressEvent.progress(model, callbackContext));

        verify(certificate, times(1)).delete(any());
        verify(certificate, times(1)).isStabilizedDelete();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testPreUpdate() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testCeritificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        try {
            testCeritificateHandler.preUpdate(ProgressEvent.progress(model, callbackContext));
            fail();
        } catch (UnsupportedOperationException ex) {
            // This operation is not supported.
        }
    }

    @Test
    public void testUpdate() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testCeritificateHandler = spy(new CertificateHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        try {
            testCeritificateHandler.update(ProgressEvent.progress(model, callbackContext));
            fail();
        } catch (UnsupportedOperationException ex) {
            // This operation is not supported.
        }
    }

}
