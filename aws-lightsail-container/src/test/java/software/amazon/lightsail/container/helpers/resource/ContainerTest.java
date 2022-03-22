package software.amazon.lightsail.container.helpers.resource;

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
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.container.AbstractTestBase;
import software.amazon.lightsail.container.ResourceModel;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.container.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class ContainerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    private Logger logger;
    private Container testContainer;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        logger = mock(Logger.class);

        final ResourceModel model = ResourceModel.builder().build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        testContainer = new Container(model, logger, proxyClient, resourceModelRequest);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testCreate() {
        when(sdkClient.createContainerService(any(CreateContainerServiceRequest.class)))
                .thenReturn(CreateContainerServiceResponse.builder().build());
        val result = testContainer.create(CreateContainerServiceRequest.builder().build());
        verify(sdkClient, times(1)).createContainerService(any(CreateContainerServiceRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testRead() {
        when(sdkClient.getContainerServices(any(GetContainerServicesRequest.class)))
                .thenReturn(GetContainerServicesResponse.builder().build());
        val result = testContainer.read(GetContainerServicesRequest.builder().build());
        verify(sdkClient, times(1)).getContainerServices(any(GetContainerServicesRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testDelete() {
        when(sdkClient.deleteContainerService(any(DeleteContainerServiceRequest.class)))
                .thenReturn(DeleteContainerServiceResponse.builder().build());
        val result = testContainer.delete(DeleteContainerServiceRequest.builder().build());
        verify(sdkClient, times(1)).deleteContainerService(any(DeleteContainerServiceRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testUpdate() {
        when(sdkClient.updateContainerService(any(UpdateContainerServiceRequest.class)))
                .thenReturn(UpdateContainerServiceResponse.builder().build());
        val result = testContainer.update(UpdateContainerServiceRequest.builder().build());
        verify(sdkClient, times(1)).updateContainerService(any(UpdateContainerServiceRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testIsStabilizedUpdate_stabilized() {
        when(sdkClient.getContainerServices(any(GetContainerServicesRequest.class)))
                .thenReturn(GetContainerServicesResponse.builder()
                        .containerServices(ContainerService.builder()
                                .state(ContainerServiceState.RUNNING)
                                .build()).build());
        val result = testContainer.isStabilizedUpdate();
        verify(sdkClient, times(1)).getContainerServices(any(GetContainerServicesRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedUpdate_notStabilized() {
        when(sdkClient.getContainerServices(any(GetContainerServicesRequest.class)))
                .thenReturn(GetContainerServicesResponse.builder()
                        .containerServices(ContainerService.builder()
                                .state(ContainerServiceState.DEPLOYING)
                                .build()).build());
        val result = testContainer.isStabilizedUpdate();
        verify(sdkClient, times(1)).getContainerServices(any(GetContainerServicesRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedCreate_stabilized() {
        when(sdkClient.getContainerServices(any(GetContainerServicesRequest.class)))
                .thenReturn(GetContainerServicesResponse.builder()
                        .containerServices(ContainerService.builder()
                                .state(ContainerServiceState.READY)
                                .build()).build());
        val result = testContainer.isStabilizedUpdate();
        verify(sdkClient, times(1)).getContainerServices(any(GetContainerServicesRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedCreate_notStabilized() {
        when(sdkClient.getContainerServices(any(GetContainerServicesRequest.class)))
                .thenReturn(GetContainerServicesResponse.builder()
                        .containerServices(ContainerService.builder()
                                .state(ContainerServiceState.PENDING)
                                .build()).build());
        val result = testContainer.isStabilizedUpdate();
        verify(sdkClient, times(1)).getContainerServices(any(GetContainerServicesRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedDelete_stabilized() {
        when(sdkClient.getContainerServices(any(GetContainerServicesRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());
        val result = testContainer.isStabilizedDelete();
        verify(sdkClient, times(1)).getContainerServices(any(GetContainerServicesRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedDelete_notStabilized() {
        when(sdkClient.getContainerServices(any(GetContainerServicesRequest.class)))
                .thenReturn(GetContainerServicesResponse.builder()
                        .containerServices(ContainerService.builder()
                                .state("Pending")
                                .build()).build());
        val result = testContainer.isStabilizedDelete();
        verify(sdkClient, times(1)).getContainerServices(any(GetContainerServicesRequest.class));
        assertThat(result).isFalse();
    }
}
