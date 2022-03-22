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
import software.amazon.awssdk.services.lightsail.model.GetCertificatesResponse;
import software.amazon.awssdk.services.lightsail.model.NotFoundException;
import software.amazon.awssdk.services.lightsail.model.TagResourceResponse;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.certificate.*;
import software.amazon.lightsail.certificate.helpers.resource.Certificate;
import software.amazon.lightsail.certificate.helpers.resource.Tags;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.certificate.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class TagsHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    @Mock
    private Certificate certificate;

    @Mock
    private Tags tag;

    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        certificate = mock(Certificate.class);
        tag = mock(Tags.class);
        logger = mock(Logger.class);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testPreUpdate() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testTagsHandler = spy(new TagsHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testTagsHandler).getCertificate(any(), any(), any());

        when(certificate.read(any()))
                .thenReturn(GetCertificatesResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testTagsHandler.preUpdate(ProgressEvent.progress(model, callbackContext));

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
    public void testPreUpdate_doesNotExist() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testTagsHandler = spy(new TagsHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testTagsHandler).getCertificate(any(), any(), any());

        when(certificate.read(any()))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testTagsHandler.preUpdate(ProgressEvent.progress(model, callbackContext));

        verify(certificate, times(1)).read(any());
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

        val testTagsHandler = spy(new TagsHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        doReturn(certificate)
                .when(testTagsHandler).getCertificate(any(), any(), any());
        doReturn(tag)
                .when(testTagsHandler).getTag(any(), any(), any());

        when(tag.update(any()))
                .thenReturn(TagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = testTagsHandler.update(ProgressEvent.progress(model, callbackContext));

        verify(tag, times(1)).update(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testPreCreate() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testTagsHandler = spy(new TagsHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        try {
            testTagsHandler.preCreate(ProgressEvent.progress(model, callbackContext));
            fail();
        } catch (UnsupportedOperationException ex) {
            // This operation is not supported.
        }
    }

    @Test
    public void testCreate() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testTagsHandler = spy(new TagsHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        try {
            testTagsHandler.create(ProgressEvent.progress(model, callbackContext));
            fail();
        } catch (UnsupportedOperationException ex) {
            // This operation is not supported.
        }
    }

    @Test
    public void testPreDelete() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testTagsHandler = spy(new TagsHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        try {
            testTagsHandler.preDelete(ProgressEvent.progress(model, callbackContext));
            fail();
        } catch (UnsupportedOperationException ex) {
            // This operation is not supported.
        }
    }

    @Test
    public void testDelete() {
        final CallbackContext callbackContext = new CallbackContext();
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        val testTagsHandler = spy(new TagsHandler(proxy, callbackContext, request.getDesiredResourceState(), logger,
                proxyClient, request));

        try {
            testTagsHandler.delete(ProgressEvent.progress(model, callbackContext));
            fail();
        } catch (UnsupportedOperationException ex) {
            // This operation is not supported.
        }
    }
}
