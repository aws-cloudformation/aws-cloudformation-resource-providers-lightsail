package software.amazon.lightsail.distribution.helpers.handler;

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
import software.amazon.lightsail.distribution.*;
import software.amazon.lightsail.distribution.CacheBehavior;
import software.amazon.lightsail.distribution.helpers.resource.Distribution;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.distribution.AbstractTestBase.MOCK_CREDENTIALS;
import static software.amazon.lightsail.distribution.CallbackContext.*;

@ExtendWith(MockitoExtension.class)
public class DistributionHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    @Mock
    private Distribution distribution;

    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        distribution = mock(Distribution.class);
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

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.read(any()))
                .thenReturn(GetDistributionsResponse.builder().build());

        try {
            testDistributionHandler.preCreate(ProgressEvent.progress(model, callbackContext));
            fail();
        } catch (CfnAlreadyExistsException ex) {
            // This exception is expected.
        }

        verify(distribution, times(1)).read(any());
    }

    @Test
    public void testPreCreate_resourceDoesNotExist() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("InvalidInputException")
                                .errorMessage("Requested resource not found")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.preCreate(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).read(any());
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

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        callbackContext.getIsPreCheckDone().put(PRE_CHECK_CREATE, true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.preCreate(ProgressEvent.progress(model, callbackContext));

        verify(distribution, never()).read(any());
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
        final ResourceModel model = ResourceModel.builder().defaultCacheBehavior(CacheBehavior.builder().behavior("cache").build()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.create(any()))
                .thenReturn(CreateDistributionResponse.builder().build());
        when(distribution.isStabilizedUpdate())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).create(any());
        verify(distribution, times(1)).isStabilizedUpdate();
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
        final ResourceModel model = ResourceModel.builder().defaultCacheBehavior(CacheBehavior.builder().behavior("cache").build()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("ServiceException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).create(any());
        verify(distribution, never()).isStabilizedUpdate();
        assertThat(distribution).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void testCreate_errorThrottling() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().defaultCacheBehavior(CacheBehavior.builder().behavior("cache").build()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("ThrottlingException").build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).create(any());
        verify(distribution, never()).isStabilizedUpdate();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void testCreate_errorAuth() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().defaultCacheBehavior(CacheBehavior.builder().behavior("cache").build()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDeniedException").build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).create(any());
        verify(distribution, never()).isStabilizedUpdate();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }

    @Test
    public void testCreate_errorRandom() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().defaultCacheBehavior(CacheBehavior.builder().behavior("cache").build()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.create(any()))
                .thenThrow(RuntimeException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).create(any());
        verify(distribution, never()).isStabilizedUpdate();
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

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.read(any()))
                .thenReturn(GetDistributionsResponse.builder().build());
        when(distribution.isStabilizedUpdate())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.preUpdate(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).read(any());
        verify(distribution, times(1)).isStabilizedUpdate();
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

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("InvalidInputException")
                                .errorMessage("Requested resource not found")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.preUpdate(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).read(any());
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

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testDistributionHandler).updateDistribution(any());
        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testDistributionHandler).updateDistributionBundle(any());
        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testDistributionHandler).detachCertificate(any());
        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testDistributionHandler).attachCertificate(any());

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.update(ProgressEvent.progress(model, callbackContext));

        verify(testDistributionHandler, times(1)).updateDistribution(any());
        verify(testDistributionHandler, times(1)).updateDistributionBundle(any());
        verify(testDistributionHandler, times(1)).detachCertificate(any());
        verify(testDistributionHandler, times(1)).attachCertificate(any());
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

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.read(any()))
                .thenReturn(GetDistributionsResponse.builder().build());
        when(distribution.isStabilizedUpdate())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.preDelete(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).read(any());
        verify(distribution, times(1)).isStabilizedUpdate();
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

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("InvalidInputException")
                                .errorMessage("Requested resource not found")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.preDelete(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).read(any());
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

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.delete(any()))
                .thenReturn(DeleteDistributionResponse.builder().build());
        when(distribution.isStabilizedDelete())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.delete(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).delete(any());
        verify(distribution, times(1)).isStabilizedDelete();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testUpdateDistribution() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().defaultCacheBehavior(CacheBehavior.builder().behavior("cache").build()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.update(any()))
                .thenReturn(UpdateDistributionResponse.builder().build());
        when(distribution.isStabilizedUpdate())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.updateDistribution(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).update(any());
        verify(distribution, times(1)).isStabilizedUpdate();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testUpdateDistributionBundle() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().defaultCacheBehavior(CacheBehavior.builder().behavior("cache").build()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.updateBundle(any()))
                .thenReturn(UpdateDistributionBundleResponse.builder().build());
        when(distribution.isStabilizedUpdate())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.updateDistributionBundle(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).updateBundle(any());
        verify(distribution, times(1)).isStabilizedUpdate();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testDetachCertificate() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.detachCertificate(any()))
                .thenReturn(DetachCertificateFromDistributionResponse.builder().build());
        when(distribution.isStabilizedUpdate())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.detachCertificate(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).detachCertificate(any());
        verify(distribution, times(1)).isStabilizedUpdate();
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

        val testDistributionHandler = spy(new DistributionHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(distribution)
                .when(testDistributionHandler).getDistribution(any(), any(), any());

        when(distribution.attachCertificate(any()))
                .thenReturn(AttachCertificateToDistributionResponse.builder().build());
        when(distribution.isStabilizedUpdate())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testDistributionHandler.attachCertificate(ProgressEvent.progress(model, callbackContext));

        verify(distribution, times(1)).attachCertificate(any());
        verify(distribution, times(1)).isStabilizedUpdate();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

}
