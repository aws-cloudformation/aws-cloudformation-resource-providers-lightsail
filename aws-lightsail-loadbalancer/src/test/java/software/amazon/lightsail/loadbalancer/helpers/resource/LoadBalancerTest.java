package software.amazon.lightsail.loadbalancer.helpers.resource;

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
import software.amazon.lightsail.loadbalancer.AbstractTestBase;
import software.amazon.lightsail.loadbalancer.ResourceModel;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.loadbalancer.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    private Logger logger;
    private LoadBalancer testLoadBalancer;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        logger = mock(Logger.class);

        final ResourceModel model = ResourceModel.builder()
                .attachedInstances(new HashSet<>(Arrays.asList("instance1"))).healthCheckPath("/old")
                .sessionStickinessEnabled(true).sessionStickinessLBCookieDurationSeconds("1000")
                .build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        testLoadBalancer = new LoadBalancer(model, logger, proxyClient, resourceModelRequest);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testCreate() {
        when(sdkClient.createLoadBalancer(any(CreateLoadBalancerRequest.class)))
                .thenReturn(CreateLoadBalancerResponse.builder().build());
        val result = testLoadBalancer.create(CreateLoadBalancerRequest.builder().build());
        verify(sdkClient, times(1)).createLoadBalancer(any(CreateLoadBalancerRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testRead() {
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder().build());
        val result = testLoadBalancer.read(GetLoadBalancerRequest.builder().build());
        verify(sdkClient, times(1)).getLoadBalancer(any(GetLoadBalancerRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testUpdateAttributes_noUpdate() {
        Map<LoadBalancerAttributeName, String> configOptions = new HashMap<>();
        configOptions.put(LoadBalancerAttributeName.SESSION_STICKINESS_ENABLED, "true");
        configOptions.put(LoadBalancerAttributeName.SESSION_STICKINESS_LB_COOKIE_DURATION_SECONDS, "1000");
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder()
                        .loadBalancer(software.amazon.awssdk.services.lightsail.model.LoadBalancer.builder()
                                .healthCheckPath("/old").configurationOptions(configOptions)
                                .build()).build());
        val result = testLoadBalancer.updateAttributes(GetLoadBalancerRequest.builder().build());
        verify(sdkClient, times(1)).getLoadBalancer(any(GetLoadBalancerRequest.class));
        verify(sdkClient, never()).updateLoadBalancerAttribute(any(UpdateLoadBalancerAttributeRequest.class));
    }

    @Test
    public void testUpdateAttributes() {
        Map<LoadBalancerAttributeName, String> configOptions = new HashMap<>();
        configOptions.put(LoadBalancerAttributeName.SESSION_STICKINESS_ENABLED, "false");
        configOptions.put(LoadBalancerAttributeName.SESSION_STICKINESS_LB_COOKIE_DURATION_SECONDS, "999");
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder()
                        .loadBalancer(software.amazon.awssdk.services.lightsail.model.LoadBalancer.builder()
                                .healthCheckPath("/new").configurationOptions(configOptions)
                                .build()).build());
        val result = testLoadBalancer.updateAttributes(GetLoadBalancerRequest.builder().build());
        verify(sdkClient, times(1)).getLoadBalancer(any(GetLoadBalancerRequest.class));
        verify(sdkClient, times(3)).updateLoadBalancerAttribute(any(UpdateLoadBalancerAttributeRequest.class));
    }

    @Test
    public void testDetachInstances() {
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder()
                                .loadBalancer(software.amazon.awssdk.services.lightsail.model.LoadBalancer.builder()
                                        .instanceHealthSummary(new HashSet<>(Arrays.asList(InstanceHealthSummary.builder().instanceName("instance3").build())))
                                        .build()).build());
        val result = testLoadBalancer.detachInstances(GetLoadBalancerRequest.builder().build());
        verify(sdkClient, times(1)).getLoadBalancer(any(GetLoadBalancerRequest.class));
        verify(sdkClient, times(1)).detachInstancesFromLoadBalancer(any(DetachInstancesFromLoadBalancerRequest.class));
    }

    @Test
    public void testDetachInstances_noDetach() {
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder()
                        .loadBalancer(software.amazon.awssdk.services.lightsail.model.LoadBalancer.builder()
                                .instanceHealthSummary(new HashSet<>(Arrays.asList(InstanceHealthSummary.builder().instanceName("instance1").build())))
                                .build()).build());
        val result = testLoadBalancer.detachInstances(GetLoadBalancerRequest.builder().build());
        verify(sdkClient, times(1)).getLoadBalancer(any(GetLoadBalancerRequest.class));
        verify(sdkClient, never()).detachInstancesFromLoadBalancer(any(DetachInstancesFromLoadBalancerRequest.class));
    }

    @Test
    public void testAttachInstances() {
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder()
                        .loadBalancer(software.amazon.awssdk.services.lightsail.model.LoadBalancer.builder()
                                .instanceHealthSummary(new HashSet<>(Arrays.asList(InstanceHealthSummary.builder().instanceName("instance3").build())))
                                .build()).build());
        val result = testLoadBalancer.attachInstances(GetLoadBalancerRequest.builder().build());
        verify(sdkClient, times(1)).getLoadBalancer(any(GetLoadBalancerRequest.class));
        verify(sdkClient, times(1)).attachInstancesToLoadBalancer(any(AttachInstancesToLoadBalancerRequest.class));
    }

    @Test
    public void testAttachInstances_noAttach() {
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder()
                        .loadBalancer(software.amazon.awssdk.services.lightsail.model.LoadBalancer.builder()
                                .instanceHealthSummary(new HashSet<>(Arrays.asList(InstanceHealthSummary.builder().instanceName("instance1").build())))
                                .build()).build());
        val result = testLoadBalancer.attachInstances(GetLoadBalancerRequest.builder().build());
        verify(sdkClient, times(1)).getLoadBalancer(any(GetLoadBalancerRequest.class));
        verify(sdkClient, never()).attachInstancesToLoadBalancer(any(AttachInstancesToLoadBalancerRequest.class));
    }

    @Test
    public void testDelete() {
        when(sdkClient.deleteLoadBalancer(any(DeleteLoadBalancerRequest.class)))
                .thenReturn(DeleteLoadBalancerResponse.builder().build());
        val result = testLoadBalancer.delete(DeleteLoadBalancerRequest.builder().build());
        verify(sdkClient, times(1)).deleteLoadBalancer(any(DeleteLoadBalancerRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testIsStabilizedDelete_stabilized() {
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());
        val result = testLoadBalancer.isStabilizedDelete();
        verify(sdkClient, times(1)).getLoadBalancer(any(GetLoadBalancerRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedDelete_notStabilized() {
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder()
                        .loadBalancer(software.amazon.awssdk.services.lightsail.model.LoadBalancer.builder()
                                .state(LoadBalancerState.UNKNOWN)
                                .build()).build());
        val result = testLoadBalancer.isStabilizedDelete();
        verify(sdkClient, times(1)).getLoadBalancer(any(GetLoadBalancerRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedInstances_Stabilized() {
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder()
                        .loadBalancer(software.amazon.awssdk.services.lightsail.model.LoadBalancer.builder()
                                .instanceHealthSummary(InstanceHealthSummary.builder().instanceHealth(InstanceHealthState.HEALTHY).build())
                                .build()).build());
        val result = testLoadBalancer.isStabilizedInstances();
        verify(sdkClient, times(1)).getLoadBalancer(any(GetLoadBalancerRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedInstances_notStabilized() {
        when(sdkClient.getLoadBalancer(any(GetLoadBalancerRequest.class)))
                .thenReturn(GetLoadBalancerResponse.builder()
                        .loadBalancer(software.amazon.awssdk.services.lightsail.model.LoadBalancer.builder()
                                .instanceHealthSummary(InstanceHealthSummary.builder().instanceHealth(InstanceHealthState.INITIAL).build())
                                .build()).build());
        val result = testLoadBalancer.isStabilizedInstances();
        verify(sdkClient, times(1)).getLoadBalancer(any(GetLoadBalancerRequest.class));
        assertThat(result).isFalse();
    }
}
