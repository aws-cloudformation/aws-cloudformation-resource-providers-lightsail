package software.amazon.lightsail.distribution.helpers.resource;

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
import software.amazon.lightsail.distribution.AbstractTestBase;
import software.amazon.lightsail.distribution.ResourceModel;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.distribution.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class DistributionTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    private Logger logger;
    private Distribution testDistribution;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        logger = mock(Logger.class);

        final ResourceModel model = ResourceModel.builder().bundleId("small_1_0").certificateName("OldCert").build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        testDistribution = new Distribution(model, logger, proxyClient, resourceModelRequest);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testCreate() {
        when(sdkClient.createDistribution(any(CreateDistributionRequest.class)))
                .thenReturn(CreateDistributionResponse.builder().build());
        val result = testDistribution.create(CreateDistributionRequest.builder().build());
        verify(sdkClient, times(1)).createDistribution(any(CreateDistributionRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testRead() {
        when(sdkClient.getDistributions(any(GetDistributionsRequest.class)))
                .thenReturn(GetDistributionsResponse.builder().build());
        val result = testDistribution.read(GetDistributionsRequest.builder().build());
        verify(sdkClient, times(1)).getDistributions(any(GetDistributionsRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testDetachCertificate_detach() {
        when(sdkClient.getDistributions(any(GetDistributionsRequest.class)))
                .thenReturn(GetDistributionsResponse.builder()
                                .distributions(LightsailDistribution.builder().certificateName("NewCert").build()).build());
        val result = testDistribution.detachCertificate(DetachCertificateFromDistributionRequest.builder().build());
        verify(sdkClient, times(1)).getDistributions(any(GetDistributionsRequest.class));
        verify(sdkClient, times(1)).detachCertificateFromDistribution(any(DetachCertificateFromDistributionRequest.class));
    }

    @Test
    public void testDetachCertificate_noDetach() {
        when(sdkClient.getDistributions(any(GetDistributionsRequest.class)))
                .thenReturn(GetDistributionsResponse.builder()
                        .distributions(LightsailDistribution.builder().certificateName("OldCert").build()).build());
        val result = testDistribution.detachCertificate(DetachCertificateFromDistributionRequest.builder().build());
        verify(sdkClient, times(1)).getDistributions(any(GetDistributionsRequest.class));
        verify(sdkClient, never()).detachCertificateFromDistribution(any(DetachCertificateFromDistributionRequest.class));
    }

    @Test
    public void testAttachCertificate_attach() {
        when(sdkClient.getDistributions(any(GetDistributionsRequest.class)))
                .thenReturn(GetDistributionsResponse.builder()
                        .distributions(LightsailDistribution.builder().certificateName("NewCert").build()).build());
        val result = testDistribution.attachCertificate(AttachCertificateToDistributionRequest.builder().build());
        verify(sdkClient, times(1)).getDistributions(any(GetDistributionsRequest.class));
        verify(sdkClient, times(1)).attachCertificateToDistribution(any(AttachCertificateToDistributionRequest.class));
    }

    @Test
    public void testAttachCertificate_noAttach() {
        when(sdkClient.getDistributions(any(GetDistributionsRequest.class)))
                .thenReturn(GetDistributionsResponse.builder()
                        .distributions(LightsailDistribution.builder().certificateName("OldCert").build()).build());
        val result = testDistribution.detachCertificate(AttachCertificateToDistributionRequest.builder().build());
        verify(sdkClient, times(1)).getDistributions(any(GetDistributionsRequest.class));
        verify(sdkClient, never()).attachCertificateToDistribution(any(AttachCertificateToDistributionRequest.class));
    }

    @Test
    public void testDelete() {
        when(sdkClient.deleteDistribution(any(DeleteDistributionRequest.class)))
                .thenReturn(DeleteDistributionResponse.builder().build());
        val result = testDistribution.delete(DeleteDistributionRequest.builder().build());
        verify(sdkClient, times(1)).deleteDistribution(any(DeleteDistributionRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testUpdate() {
        when(sdkClient.updateDistribution(any(UpdateDistributionRequest.class)))
                .thenReturn(UpdateDistributionResponse.builder().build());
        val result = testDistribution.update(UpdateDistributionRequest.builder().build());
        verify(sdkClient, times(1)).updateDistribution(any(UpdateDistributionRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testUpdateBundle_noUpdateRequired() {
        when(sdkClient.getDistributions(any(GetDistributionsRequest.class)))
                .thenReturn(GetDistributionsResponse.builder()
                        .distributions(LightsailDistribution.builder()
                                .bundleId("small_1_0")
                                .build()).build());
        val result = testDistribution.updateBundle(UpdateDistributionRequest.builder().build());
        verify(sdkClient, never()).updateDistributionBundle(any(UpdateDistributionBundleRequest.class));
    }

    @Test
    public void testUpdateBundle_updateRequired() {
        when(sdkClient.getDistributions(any(GetDistributionsRequest.class)))
                .thenReturn(GetDistributionsResponse.builder()
                        .distributions(LightsailDistribution.builder()
                                .bundleId("large_1_0")
                                .build()).build());
        when(sdkClient.updateDistributionBundle(any(UpdateDistributionBundleRequest.class)))
                .thenReturn(UpdateDistributionBundleResponse.builder().build());
        val result = testDistribution.updateBundle(UpdateDistributionBundleRequest.builder().build());
        verify(sdkClient, times(1)).updateDistributionBundle(any(UpdateDistributionBundleRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testIsStabilizedUpdate_stabilized() {
        when(sdkClient.getDistributions(any(GetDistributionsRequest.class)))
                .thenReturn(GetDistributionsResponse.builder()
                        .distributions(LightsailDistribution.builder()
                                .status("Deployed")
                                .build()).build());
        val result = testDistribution.isStabilizedUpdate();
        verify(sdkClient, times(1)).getDistributions(any(GetDistributionsRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedUpdate_notStabilized() {
        when(sdkClient.getDistributions(any(GetDistributionsRequest.class)))
                .thenReturn(GetDistributionsResponse.builder()
                        .distributions(LightsailDistribution.builder()
                                .status("In Progress")
                                .build()).build());
        val result = testDistribution.isStabilizedUpdate();
        verify(sdkClient, times(1)).getDistributions(any(GetDistributionsRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedDelete_stabilized() {
        when(sdkClient.getDistributions(any(GetDistributionsRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("InvalidInputException")
                                .build()).build());
        val result = testDistribution.isStabilizedDelete();
        verify(sdkClient, times(1)).getDistributions(any(GetDistributionsRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedDelete_notStabilized() {
        when(sdkClient.getDistributions(any(GetDistributionsRequest.class)))
                .thenReturn(GetDistributionsResponse.builder()
                        .distributions(LightsailDistribution.builder()
                                .status("In Progress")
                                .build()).build());
        val result = testDistribution.isStabilizedDelete();
        verify(sdkClient, times(1)).getDistributions(any(GetDistributionsRequest.class));
        assertThat(result).isFalse();
    }
}
