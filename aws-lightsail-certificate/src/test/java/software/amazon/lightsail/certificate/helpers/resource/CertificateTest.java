package software.amazon.lightsail.certificate.helpers.resource;

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
import software.amazon.lightsail.certificate.AbstractTestBase;
import software.amazon.lightsail.certificate.ResourceModel;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.certificate.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class CertificateTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    private Logger logger;
    private Certificate testCertificate;

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
        testCertificate = new Certificate(model, logger, proxyClient, resourceModelRequest);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testCreate() {
        when(sdkClient.createCertificate(any(CreateCertificateRequest.class)))
                .thenReturn(CreateCertificateResponse.builder().build());
        val result = testCertificate.create(CreateCertificateRequest.builder().build());
        verify(sdkClient, times(1)).createCertificate(any(CreateCertificateRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testRead() {
        when(sdkClient.getCertificates(any(GetCertificatesRequest.class)))
                .thenReturn(GetCertificatesResponse.builder().certificates(CertificateSummary.builder().build()).build());
        val result = testCertificate.read(GetCertificatesRequest.builder().build());
        verify(sdkClient, times(1)).getCertificates(any(GetCertificatesRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testDelete() {
        when(sdkClient.deleteCertificate(any(DeleteCertificateRequest.class)))
                .thenReturn(DeleteCertificateResponse.builder().build());
        val result = testCertificate.delete(DeleteCertificateRequest.builder().build());
        verify(sdkClient, times(1)).deleteCertificate(any(DeleteCertificateRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testIsStabilizedCreate_stabilized() {
        when(sdkClient.getCertificates(any(GetCertificatesRequest.class)))
                .thenReturn(GetCertificatesResponse.builder().certificates(CertificateSummary.builder().build()).build());
        val result = testCertificate.isStabilizedCreate();
        verify(sdkClient, times(1)).getCertificates(any(GetCertificatesRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedCreate_notStabilized() {
        when(sdkClient.getCertificates(any(GetCertificatesRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());
        val result = testCertificate.isStabilizedCreate();
        verify(sdkClient, times(1)).getCertificates(any(GetCertificatesRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedDelete_stabilized() {
        when(sdkClient.getCertificates(any(GetCertificatesRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());
        val result = testCertificate.isStabilizedDelete();
        verify(sdkClient, times(1)).getCertificates(any(GetCertificatesRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedDelete_notStabilized() {
        when(sdkClient.getCertificates(any(GetCertificatesRequest.class)))
                .thenReturn(GetCertificatesResponse.builder()
                        .certificates(CertificateSummary.builder().build()).build());
        val result = testCertificate.isStabilizedDelete();
        verify(sdkClient, times(1)).getCertificates(any(GetCertificatesRequest.class));
        assertThat(result).isFalse();
    }

}
