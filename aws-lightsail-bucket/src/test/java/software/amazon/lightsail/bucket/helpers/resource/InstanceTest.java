package software.amazon.lightsail.bucket.helpers.resource;

import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceResponse;
import software.amazon.awssdk.services.lightsail.model.InstanceState;
import software.amazon.awssdk.services.lightsail.model.NotFoundException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.bucket.AbstractTestBase;
import software.amazon.lightsail.bucket.ResourceModel;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.bucket.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class InstanceTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        logger = mock(Logger.class);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testRead() {
        ResourceModel model = ResourceModel.builder().build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        Instance testInstance = new Instance(model, logger, proxyClient, resourceModelRequest);

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(GetInstanceResponse.builder().build());
        val result = testInstance.read("resource");
        verify(sdkClient, times(1)).getInstance(any(GetInstanceRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testIsStabilized_stabilized() {
        ResourceModel model = ResourceModel.builder().build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        Instance testInstance = new Instance(model, logger, proxyClient, resourceModelRequest);

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(GetInstanceResponse.builder()
                        .instance(software.amazon.awssdk.services.lightsail.model.Instance.builder()
                                .state(InstanceState.builder()
                                        .name("Running")
                                        .build()).build()).build());
        val result = testInstance.isStabilized("resource");
        verify(sdkClient, times(1)).getInstance(any(GetInstanceRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilized_notStabilized() {
        ResourceModel model = ResourceModel.builder().build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        Instance testInstance = new Instance(model, logger, proxyClient, resourceModelRequest);

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(GetInstanceResponse.builder()
                        .instance(software.amazon.awssdk.services.lightsail.model.Instance.builder()
                                .state(InstanceState.builder()
                                        .name("Pending")
                                        .build()).build()).build());
        val result = testInstance.isStabilized("resource");
        verify(sdkClient, times(1)).getInstance(any(GetInstanceRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testUnSupportedMethods() {
        ResourceModel model = ResourceModel.builder().build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        Instance testInstance = new Instance(model, logger, proxyClient, resourceModelRequest);
        try {
            testInstance.create(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testInstance.delete(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testInstance.update(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testInstance.isStabilizedUpdate();
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testInstance.isStabilizedDelete();
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testInstance.isSafeExceptionCreateOrUpdate(NotFoundException.builder().build());
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testInstance.isSafeExceptionDelete(NotFoundException.builder().build());
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }
}