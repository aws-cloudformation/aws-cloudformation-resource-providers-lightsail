package software.amazon.lightsail.instance.helpers.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.AddOnType;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.lightsail.instance.AbstractTestBase;
import software.amazon.lightsail.instance.AddOn;
import software.amazon.lightsail.instance.AutoSnapshotAddOn;
import software.amazon.lightsail.instance.CallbackContext;
import software.amazon.lightsail.instance.Networking;
import software.amazon.lightsail.instance.Port;
import software.amazon.lightsail.instance.ResourceModel;
import software.amazon.lightsail.instance.State;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static software.amazon.lightsail.instance.AbstractTestBase.MOCK_CREDENTIALS;

class InstanceHandlerTest {

    private InstanceHandler testInstance;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    private LightsailClient sdkClient;

    private ProgressEvent<ResourceModel, CallbackContext> progressEvent;
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
        ProxyClient<LightsailClient> proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        testInstance = new InstanceHandler(mock(AmazonWebServicesClientProxy.class), mock(CallbackContext.class),
                model, logger, proxyClient, null);
        progressEvent = ProgressEvent.defaultSuccessHandler(null);
    }

    @Test
    public void testUpdate() {
        try {
            testInstance.update(progressEvent);
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }

    @Test
    public void testPreUpdate() {
        try {
            testInstance.preUpdate(progressEvent);
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }
}
