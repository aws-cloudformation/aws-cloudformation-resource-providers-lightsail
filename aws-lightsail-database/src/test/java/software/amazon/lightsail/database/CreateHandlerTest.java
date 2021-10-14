package software.amazon.lightsail.database;

import java.time.Duration;

import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.proxy.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.lightsail.database.helpers.handler.DatabaseHandler;
import software.amazon.lightsail.database.helpers.resource.Database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    @Mock
    private DatabaseHandler databaseHandler;

    @Mock
    private Database database;

    @Mock
    private UpdateHandler updateHandler;

    private CreateHandler createHandler;
    private CallbackContext callbackContext;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        databaseHandler = mock(DatabaseHandler.class);
        database = mock(Database.class);
        updateHandler = mock(UpdateHandler.class);

        createHandler = spy(new CreateHandler());
        callbackContext = new CallbackContext();

        doReturn(databaseHandler)
                .when(createHandler).getDatabaseHandler(any(), any(), any(), any(), any());
        doReturn(database)
                .when(createHandler).getDatabase(any(), any(), any());
        doReturn(updateHandler)
                .when(createHandler).getUpdateHandler();
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void handleRequest_modelWithoutAz() {

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        when(databaseHandler.handleCreate(any()))
                .thenReturn(ProgressEvent.progress(model, callbackContext));
        when(updateHandler.handleRequest(any(), any(), any(), any(), any()))
                    .thenReturn(ProgressEvent.success(model, callbackContext));

        final ProgressEvent<ResourceModel, CallbackContext> response = createHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        verify(databaseHandler, times(1)).handleCreate(ProgressEvent.progress(model, callbackContext));
        verify(database, times(1)).getFirstAvailabilityZone();
        verify(updateHandler, times(1)).handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_modelWithAz() {

        final ResourceModel model = ResourceModel.builder()
                .availabilityZone("us-west-2")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(databaseHandler.handleCreate(any()))
                .thenReturn(ProgressEvent.progress(model, callbackContext));
        when(updateHandler.handleRequest(any(), any(), any(), any(), any()))
                .thenReturn(ProgressEvent.success(model, callbackContext));

        final ProgressEvent<ResourceModel, CallbackContext> response = createHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        verify(databaseHandler, times(1)).handleCreate(ProgressEvent.progress(model, callbackContext));
        verify(database, never()).getFirstAvailabilityZone();
        verify(updateHandler, times(1)).handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
