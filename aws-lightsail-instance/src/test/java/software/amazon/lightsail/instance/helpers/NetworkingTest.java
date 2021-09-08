package software.amazon.lightsail.instance.helpers;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.AddOnType;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceResponse;
import software.amazon.awssdk.services.lightsail.model.Instance;
import software.amazon.awssdk.services.lightsail.model.InstanceState;
import software.amazon.awssdk.services.lightsail.model.NotFoundException;
import software.amazon.awssdk.services.lightsail.model.PutInstancePublicPortsRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.lightsail.instance.AbstractTestBase;
import software.amazon.lightsail.instance.Networking;
import software.amazon.lightsail.instance.Port;
import software.amazon.lightsail.instance.ResourceModel;
import software.amazon.lightsail.instance.State;

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

class NetworkingTest {

    private software.amazon.lightsail.instance.helpers.resource.Networking networkingTest;

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
                .networking(Networking.builder()
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
        networkingTest = new software.amazon.lightsail.instance.helpers.resource.Networking(model, logger, proxyClient, null);
    }

    @Test
    public void testUpdate() {
        val resourceModel = GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
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
        networkingTest.update(PutInstancePublicPortsRequest.builder().build());
        verify(sdkClient, times(1))
                .putInstancePublicPorts(any(PutInstancePublicPortsRequest.class));
    }

    @Test
    public void testUnSupportedMethods() {
        try {
            networkingTest.create(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            networkingTest.delete(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            networkingTest.read(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            networkingTest.isStabilizedUpdate();
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            networkingTest.isStabilizedDelete();
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            networkingTest.isSafeExceptionCreateOrUpdate(NotFoundException.builder().build());
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            networkingTest.isSafeExceptionDelete(NotFoundException.builder().build());
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }
}
