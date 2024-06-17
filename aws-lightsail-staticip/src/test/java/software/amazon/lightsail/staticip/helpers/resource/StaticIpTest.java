package software.amazon.lightsail.staticip.helpers.resource;

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
import software.amazon.lightsail.staticip.AbstractTestBase;
import software.amazon.lightsail.staticip.ResourceModel;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.staticip.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class StaticIpTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    private Logger logger;
    private StaticIp testStaticIp;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        logger = mock(Logger.class);

        final ResourceModel model = ResourceModel.builder().build();
        testStaticIp = new StaticIp(model, logger, proxyClient, null);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testCreate() {
        when(sdkClient.allocateStaticIp(any(AllocateStaticIpRequest.class)))
                .thenReturn(AllocateStaticIpResponse.builder().build());
        val result = testStaticIp.create(AllocateStaticIpRequest.builder().build());
        verify(sdkClient, times(1)).allocateStaticIp(any(AllocateStaticIpRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testRead() {
        when(sdkClient.getStaticIp(any(GetStaticIpRequest.class)))
                .thenReturn(GetStaticIpResponse.builder().build());
        val result = testStaticIp.read(GetStaticIpRequest.builder().build());
        verify(sdkClient, times(1)).getStaticIp(any(GetStaticIpRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testDelete() {
        when(sdkClient.releaseStaticIp(any(ReleaseStaticIpRequest.class)))
                .thenReturn(ReleaseStaticIpResponse.builder().build());
        val result = testStaticIp.delete(ReleaseStaticIpRequest.builder().build());
        verify(sdkClient, times(1)).releaseStaticIp(any(ReleaseStaticIpRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testIsStabilizedCreate_stabilized() {
        when(sdkClient.getStaticIp(any(GetStaticIpRequest.class)))
                .thenReturn(GetStaticIpResponse.builder()
                        .staticIp(software.amazon.awssdk.services.lightsail.model.StaticIp.builder()
                                .build()).build());
        val result = testStaticIp.isStabilizedCreate();
        verify(sdkClient, times(1)).getStaticIp(any(GetStaticIpRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedCreate_notStabilized() {
        when(sdkClient.getStaticIp(any(GetStaticIpRequest.class)))
                .thenThrow(NotFoundException.builder()
                    .awsErrorDetails(AwsErrorDetails
                            .builder().errorCode("NotFoundException")
                            .build()).build());
        val result = testStaticIp.isStabilizedCreate();
        verify(sdkClient, times(1)).getStaticIp(any(GetStaticIpRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedDelete_stabilized() {
        when(sdkClient.getStaticIp(any(GetStaticIpRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());
        val result = testStaticIp.isStabilizedDelete();
        verify(sdkClient, times(1)).getStaticIp(any(GetStaticIpRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedDelete_notStabilized() {
        when(sdkClient.getStaticIp(any(GetStaticIpRequest.class)))
                .thenReturn(GetStaticIpResponse.builder()
                        .staticIp(software.amazon.awssdk.services.lightsail.model.StaticIp.builder()
                                .build()).build());
        val result = testStaticIp.isStabilizedDelete();
        verify(sdkClient, times(1)).getStaticIp(any(GetStaticIpRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void detach_Required() {
        ResourceModel resourceModel = ResourceModel.builder().build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        StaticIp testStaticIp1 = new StaticIp(resourceModel, logger, proxyClient, resourceModelRequest);

        when(sdkClient.getStaticIp(any(GetStaticIpRequest.class)))
                .thenReturn(GetStaticIpResponse.builder()
                        .staticIp(software.amazon.awssdk.services.lightsail.model.StaticIp.builder()
                                .isAttached(true).build()).build());
        when(sdkClient.detachStaticIp(any(DetachStaticIpRequest.class)))
                .thenReturn(DetachStaticIpResponse.builder().build());

        val result = testStaticIp1.detach();
        verify(sdkClient, times(1)).getStaticIp(any(GetStaticIpRequest.class));
        verify(sdkClient, times(1)).detachStaticIp(any(DetachStaticIpRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void detach_notRequired() {
        ResourceModel resourceModel = ResourceModel.builder().attachedTo("testInstance").build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        StaticIp testStaticIp1 = new StaticIp(resourceModel, logger, proxyClient, resourceModelRequest);

        when(sdkClient.getStaticIp(any(GetStaticIpRequest.class)))
                .thenReturn(GetStaticIpResponse.builder()
                        .staticIp(software.amazon.awssdk.services.lightsail.model.StaticIp.builder()
                                .build()).build());

        val result = testStaticIp1.detach();
        verify(sdkClient, times(1)).getStaticIp(any(GetStaticIpRequest.class));
        verify(sdkClient, never()).detachStaticIp(any(DetachStaticIpRequest.class));
        assertThat(result).isNull();
    }

    @Test
    public void attach_Required() {
        ResourceModel resourceModel = ResourceModel.builder().attachedTo("testInstance").build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        StaticIp testStaticIp1 = new StaticIp(resourceModel, logger, proxyClient, resourceModelRequest);

        when(sdkClient.getStaticIp(any(GetStaticIpRequest.class)))
                .thenReturn(GetStaticIpResponse.builder()
                        .staticIp(software.amazon.awssdk.services.lightsail.model.StaticIp.builder()
                                .build()).build());
        when(sdkClient.attachStaticIp(any(AttachStaticIpRequest.class)))
                .thenReturn(AttachStaticIpResponse.builder().build());

        val result = testStaticIp1.attach();
        verify(sdkClient, times(1)).getStaticIp(any(GetStaticIpRequest.class));
        verify(sdkClient, times(1)).attachStaticIp(any(AttachStaticIpRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void attach_notRequired() {
        ResourceModel resourceModel = ResourceModel.builder().build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        StaticIp testStaticIp1 = new StaticIp(resourceModel, logger, proxyClient, resourceModelRequest);

        when(sdkClient.getStaticIp(any(GetStaticIpRequest.class)))
                .thenReturn(GetStaticIpResponse.builder()
                        .staticIp(software.amazon.awssdk.services.lightsail.model.StaticIp.builder()
                                .build()).build());

        val result = testStaticIp1.attach();
        verify(sdkClient, times(1)).getStaticIp(any(GetStaticIpRequest.class));
        verify(sdkClient, never()).attachStaticIp(any(AttachStaticIpRequest.class));
        assertThat(result).isNull();
    }

    @Test
    public void attach_alreadyAttached() {
        ResourceModel resourceModel = ResourceModel.builder().attachedTo("testInstance").build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .build();
        StaticIp testStaticIp1 = new StaticIp(resourceModel, logger, proxyClient, resourceModelRequest);

        when(sdkClient.getStaticIp(any(GetStaticIpRequest.class)))
                .thenReturn(GetStaticIpResponse.builder()
                        .staticIp(software.amazon.awssdk.services.lightsail.model.StaticIp.builder()
                                .attachedTo("testInstance").build()).build());

        val result = testStaticIp1.attach();
        verify(sdkClient, times(1)).getStaticIp(any(GetStaticIpRequest.class));
        verify(sdkClient, never()).attachStaticIp(any(AttachStaticIpRequest.class));
        assertThat(result).isNull();
    }

}
