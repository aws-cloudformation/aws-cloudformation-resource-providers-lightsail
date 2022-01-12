package software.amazon.lightsail.alarm;

import java.time.Duration;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lightsail.alarm.helpers.handler.AlarmHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    @Mock
    private AlarmHandler alarmHandler;

    @Mock
    private ReadHandler readHandler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        alarmHandler = mock(AlarmHandler.class);
        readHandler = mock(ReadHandler.class);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final UpdateHandler updateHandler = spy(new UpdateHandler());
        final CallbackContext callbackContext = new CallbackContext();

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        doReturn(alarmHandler)
                .when(updateHandler).getAlarmHandler(any(), any(), any(), any(), any());
        doReturn(readHandler)
                .when(updateHandler).getReadHandler();

        when(alarmHandler.handleUpdate(any()))
                .thenReturn(ProgressEvent.progress(model, callbackContext));
        when(readHandler.handleRequest(any(), any(), any(), any(), any()))
                .thenReturn(ProgressEvent.success(model, callbackContext));

        final ProgressEvent<ResourceModel, CallbackContext> response = updateHandler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(alarmHandler, times(1)).handleUpdate(ProgressEvent.progress(model, callbackContext));
        verify(readHandler, times(1)).handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
