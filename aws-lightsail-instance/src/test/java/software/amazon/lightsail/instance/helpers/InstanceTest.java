package software.amazon.lightsail.instance.helpers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.AddOnType;
import software.amazon.awssdk.services.lightsail.model.CreateInstancesRequest;
import software.amazon.awssdk.services.lightsail.model.DeleteInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceResponse;
import software.amazon.awssdk.services.lightsail.model.InstanceState;
import software.amazon.awssdk.services.lightsail.model.NotFoundException;
import software.amazon.awssdk.services.lightsail.model.StartInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.StopInstanceRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.AbstractTestBase;
import software.amazon.lightsail.instance.Port;
import software.amazon.lightsail.instance.ResourceModel;
import software.amazon.lightsail.instance.State;
import software.amazon.lightsail.instance.helpers.resource.Instance;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.lightsail.instance.AbstractTestBase.MOCK_CREDENTIALS;

class InstanceTest {

    private Instance instanceTest;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    private LightsailClient sdkClient;

    @BeforeEach
    public void setup() {

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(State.builder()
                        .name("Running")
                        .build())
                .networking(software.amazon.lightsail.instance.Networking.builder()
                        .ports(ImmutableSet.of(
                                Port.builder()
                                        .ipv6Cidrs(ImmutableList.of("2.2.24.2.3.2323.232323:/dsds"))
                                        .accessFrom("1")
                                        .cidrs(ImmutableList.of("1.2.2.232"))
                                        .accessDirection("one")
                                        .fromPort(20)
                                        .toPort(40)
                                        .cidrListAliases(ImmutableList.of("1.2.2.2"))
                                        .build()))
                        .monthlyTransfer(software.amazon.lightsail.instance.MonthlyTransfer.builder()
                                .gbPerMonthAllocated("20").build())
                        .build())
                .tags(new HashSet<>()).build();
        val logger = mock(Logger.class);
        sdkClient = mock(LightsailClient.class);
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        ProxyClient<LightsailClient> proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        instanceTest = new Instance(model, logger, proxyClient, resourceModelRequest);
    }

    @Test
    public void testCreate() {
        instanceTest.create(CreateInstancesRequest.builder().build());
        verify(sdkClient, times(1)).createInstances(any(CreateInstancesRequest.class));
    }

    @Test
    public void testDelete() {
        val resourceModel = GetInstanceResponse.builder()
                .instance(software.amazon.awssdk.services.lightsail.model.Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .build()).build();
        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(resourceModel).thenThrow(NotFoundException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NotFoundException").build())
                .build());
        instanceTest.delete(DeleteInstanceRequest.builder().build());
        verify(sdkClient, times(1)).deleteInstance(any(DeleteInstanceRequest.class));
    }

    @Test
    public void testGet() {
        instanceTest.read(GetInstanceRequest.builder().build());
        verify(sdkClient, times(1)).getInstance(any(GetInstanceRequest.class));
    }

    @Test
    public void testStop() {
        val resourceModel = GetInstanceResponse.builder()
                .instance(software.amazon.awssdk.services.lightsail.model.Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .build()).build();
        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(resourceModel);
        instanceTest.stop(StopInstanceRequest.builder().build());
        verify(sdkClient, times(1)).stopInstance(any(StopInstanceRequest.class));
    }

    @Test
    public void testStart() {
        val resourceModel = GetInstanceResponse.builder()
                .instance(software.amazon.awssdk.services.lightsail.model.Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .build()).build();
        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(resourceModel);
        instanceTest.start(StartInstanceRequest.builder().build());
        verify(sdkClient, times(1)).startInstance(any(StartInstanceRequest.class));
    }

    @Test
    public void testIsStabilizedCreateOrUpdate() {
        val resourceModel = GetInstanceResponse.builder()
                .instance(software.amazon.awssdk.services.lightsail.model.Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .build()).build();
        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(resourceModel);
        Assertions.assertTrue(instanceTest.isStabilizedUpdate());
    }

    @Test
    public void testIsStabilizedDelete() {
        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("NotFoundException").build())
                        .build());
        Assertions.assertTrue(instanceTest.isStabilizedDelete());
    }

    @Test
    public void testUnSupportedMethods() {
        try {
            instanceTest.update(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }
}
