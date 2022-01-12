package software.amazon.lightsail.bucket.helpers.handler;

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
import software.amazon.lightsail.bucket.*;
import software.amazon.lightsail.bucket.helpers.resource.Bucket;
import software.amazon.lightsail.bucket.helpers.resource.Instance;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.bucket.AbstractTestBase.MOCK_CREDENTIALS;
import static software.amazon.lightsail.bucket.CallbackContext.*;

@ExtendWith(MockitoExtension.class)
public class BucketHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    @Mock
    private Bucket bucket;

    @Mock
    private software.amazon.lightsail.bucket.helpers.resource.Instance instance;

    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        bucket = mock(Bucket.class);
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.read(any()))
                .thenReturn(GetBucketsResponse.builder().build());

        try {
            testBucketHandler.preCreate(ProgressEvent.progress(model, callbackContext));
            fail();
        } catch (CfnAlreadyExistsException ex) {
            // This exception is expected.
        }

        verify(bucket, times(1)).read(any());
    }

    @Test
    public void testPreCreate_resourceDoesNotExist() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.preCreate(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).read(any());
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        callbackContext.getIsPreCheckDone().put(PRE_CHECK_CREATE, true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.preCreate(ProgressEvent.progress(model, callbackContext));

        verify(bucket, never()).read(any());
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.create(any()))
                .thenReturn(CreateBucketResponse.builder().build());
        when(bucket.isStabilizedCreate())
                .thenReturn(true);

        callbackContext.incrementWaitCount(POST_CHECK_CREATE);

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).create(any());
        verify(bucket, times(1)).isStabilizedCreate();
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("ServiceException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).create(any());
        verify(bucket, never()).isStabilizedCreate();
        assertThat(bucket).isNotNull();
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("ThrottlingException").build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).create(any());
        verify(bucket, never()).isStabilizedCreate();
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.create(any()))
                .thenThrow(ServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDeniedException").build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).create(any());
        verify(bucket, never()).isStabilizedCreate();
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.create(any()))
                .thenThrow(RuntimeException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.create(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).create(any());
        verify(bucket, never()).isStabilizedCreate();
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.read(any()))
                .thenReturn(GetBucketsResponse.builder().build());
        when(bucket.isStabilizedCreate())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.preUpdate(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).read(any());
        verify(bucket, times(1)).isStabilizedCreate();
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.preUpdate(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).read(any());
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testBucketHandler).updateBucket(any());
        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testBucketHandler).updateBucketBundle(any());
        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testBucketHandler).preDetachInstances(any());
        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testBucketHandler).detachInstances(any());
        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testBucketHandler).preAttachInstances(any());
        doReturn(ProgressEvent.progress(model, callbackContext))
                .when(testBucketHandler).attachInstances(any());

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.update(ProgressEvent.progress(model, callbackContext));

        verify(testBucketHandler, times(1)).updateBucket(any());
        verify(testBucketHandler, times(1)).updateBucketBundle(any());
        verify(testBucketHandler, times(1)).preDetachInstances(any());
        verify(testBucketHandler, times(1)).detachInstances(any());
        verify(testBucketHandler, times(1)).preAttachInstances(any());
        verify(testBucketHandler, times(1)).attachInstances(any());
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.read(any()))
                .thenReturn(GetBucketsResponse.builder().build());
        when(bucket.isStabilizedUpdate())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.preDelete(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).read(any());
        verify(bucket, times(1)).isStabilizedUpdate();
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.preDelete(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).read(any());
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.delete(any()))
                .thenReturn(DeleteBucketResponse.builder().build());
        when(bucket.isStabilizedDelete())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.delete(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).delete(any());
        verify(bucket, times(1)).isStabilizedDelete();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testUpdateBucket() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.update(any()))
                .thenReturn(UpdateBucketResponse.builder().build());
        when(bucket.isStabilizedCreate())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.updateBucket(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).update(any());
        verify(bucket, times(1)).isStabilizedCreate();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testUpdateBucketBundle() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.updateBundle(any()))
                .thenReturn(UpdateBucketBundleResponse.builder().build());
        when(bucket.isStabilizedCreate())
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.updateBucketBundle(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).updateBundle(any());
        verify(bucket, times(1)).isStabilizedCreate();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testPreDetachInstances() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());
        doReturn(instance)
                .when(testBucketHandler).getInstance(any(), any(), any());

        when(bucket.getCurrentResourceModelFromLightsail())
                .thenReturn(ResourceModel.builder().build());
        when(bucket.setDifference(any(), any()))
                .thenReturn(new HashSet<>(Arrays.asList("resource1", "resource2")));
        when(instance.isStabilized(any()))
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.preDetachInstances(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).getCurrentResourceModelFromLightsail();
        verify(bucket, times(1)).setDifference(any(), any());
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
    public void testDetachInstances() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.detachInstances(any()))
                .thenReturn(null);
        when(bucket.isStabilizedCreate())
                .thenReturn(true);
        callbackContext.incrementWaitCount(POST_CHECK_DETACH);

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.detachInstances(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).detachInstances(any());
        verify(bucket, times(1)).isStabilizedCreate();
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());
        doReturn(instance)
                .when(testBucketHandler).getInstance(any(), any(), any());

        when(bucket.getCurrentResourceModelFromLightsail())
                .thenReturn(ResourceModel.builder().build());
        when(bucket.setDifference(any(), any()))
                .thenReturn(new HashSet<>(Arrays.asList("resource1", "resource2")));
        when(bucket.readAll(any()))
                .thenReturn(GetBucketsResponse.builder().build());
        when(instance.isStabilized(any()))
                .thenReturn(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.preAttachInstances(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).getCurrentResourceModelFromLightsail();
        verify(bucket, times(1)).setDifference(any(), any());
        verify(bucket, times(1)).readAll(any());
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

        val testBucketHandler = spy(new BucketHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(bucket)
                .when(testBucketHandler).getBucket(any(), any(), any());

        when(bucket.attachInstances(any()))
                .thenReturn(null);
        when(bucket.isStabilizedCreate())
                .thenReturn(true);
        callbackContext.incrementWaitCount(POST_CHECK_ATTACH);

        final ProgressEvent<ResourceModel, CallbackContext> response = testBucketHandler.attachInstances(ProgressEvent.progress(model, callbackContext));

        verify(bucket, times(1)).attachInstances(any());
        verify(bucket, times(1)).isStabilizedCreate();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

}
