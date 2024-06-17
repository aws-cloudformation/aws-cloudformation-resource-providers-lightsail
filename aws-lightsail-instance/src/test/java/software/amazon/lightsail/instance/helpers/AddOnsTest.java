package software.amazon.lightsail.instance.helpers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.AddOnType;
import software.amazon.awssdk.services.lightsail.model.DisableAddOnRequest;
import software.amazon.awssdk.services.lightsail.model.EnableAddOnRequest;
import software.amazon.awssdk.services.lightsail.model.NotFoundException;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceResponse;
import software.amazon.awssdk.services.lightsail.model.InvalidInputException;
import software.amazon.awssdk.services.lightsail.model.Instance;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.lightsail.instance.AbstractTestBase;
import software.amazon.lightsail.instance.AddOn;
import software.amazon.lightsail.instance.AutoSnapshotAddOn;
import software.amazon.lightsail.instance.Networking;
import software.amazon.lightsail.instance.Port;
import software.amazon.lightsail.instance.ResourceModel;
import software.amazon.lightsail.instance.State;
import software.amazon.lightsail.instance.helpers.resource.AddOns;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static software.amazon.lightsail.instance.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
class AddOnsTest {


    private AddOns testAddOns;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    private LightsailClient sdkClient;

    @BeforeEach
    public void setup() {

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(State.builder()
                        .name("Running")
                        .build())
                .addOns(ImmutableList.of(AddOn.builder()
                        .addOnType(AddOnType.AUTO_SNAPSHOT.toString())
                        .status("Disabled")
                        .autoSnapshotAddOnRequest(AutoSnapshotAddOn.builder().build())
                        .build()))
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
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        testAddOns = new AddOns(model, logger, proxyClient, null);
    }

    @AfterEach
    public void tear_down() {
    }


    @Test
    public void testUpdate_disable_not_required() {
        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(GetInstanceResponse.builder().instance(Instance.builder().build()).build());
        try {
            testAddOns.update(mock(AwsRequest.class));
        } catch(InvalidInputException ex) {
            verify(sdkClient, never()).disableAddOn(any(DisableAddOnRequest.class));
        }
    }

    @Test
    public void testUpdate_disable_required() {
        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(GetInstanceResponse.builder().instance(Instance.builder().addOns(
                                software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                        .build()).build()).build());
        testAddOns.update(mock(AwsRequest.class));
        verify(sdkClient, times(1)).disableAddOn(any(DisableAddOnRequest.class));
    }

    @Test
    public void testUpdate_enable() {
        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(State.builder()
                        .name("Running")
                        .build())
                .addOns(ImmutableList.of(AddOn.builder()
                        .addOnType(AddOnType.AUTO_SNAPSHOT.toString())
                        .status("Enabled")
                        .autoSnapshotAddOnRequest(AutoSnapshotAddOn.builder().build())
                        .build()))
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
        ProxyClient<LightsailClient> proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        testAddOns = new AddOns(model, logger, proxyClient, null);
        testAddOns.update(any(AwsRequest.class));
        verify(sdkClient, times(1)).enableAddOn(any(EnableAddOnRequest.class));
    }

    @Test
    public void testUnSupportedMethods() {
        try {
            testAddOns.isSafeExceptionDelete(NotFoundException.builder().build());
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testAddOns.isStabilizedDelete();
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }
}
