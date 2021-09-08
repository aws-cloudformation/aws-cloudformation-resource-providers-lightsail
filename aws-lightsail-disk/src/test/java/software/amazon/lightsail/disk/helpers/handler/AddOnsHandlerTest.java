package software.amazon.lightsail.disk.helpers.handler;

import com.google.common.collect.ImmutableList;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.AddOnType;
import software.amazon.awssdk.services.lightsail.model.DiskState;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.lightsail.disk.AbstractTestBase;
import software.amazon.lightsail.disk.AddOn;
import software.amazon.lightsail.disk.AutoSnapshotAddOn;
import software.amazon.lightsail.disk.CallbackContext;
import software.amazon.lightsail.disk.ResourceModel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static software.amazon.lightsail.disk.AbstractTestBase.MOCK_CREDENTIALS;

class AddOnsHandlerTest {

    private AddOnsHandler testAddOns;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    private LightsailClient sdkClient;

    private ProgressEvent<ResourceModel, CallbackContext> progressEvent;
    @BeforeEach
    public void setup() {

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(DiskState.AVAILABLE.name())
                .addOns(ImmutableList.of(AddOn.builder()
                        .addOnType(AddOnType.AUTO_SNAPSHOT.toString())
                        .status("Disabled")
                        .autoSnapshotAddOnRequest(AutoSnapshotAddOn.builder().build())
                        .build()))
                .tags(new HashSet<>()).build();
        val logger = mock(Logger.class);
        sdkClient = mock(LightsailClient.class);
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        ProxyClient<LightsailClient> proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        testAddOns = new AddOnsHandler(mock(AmazonWebServicesClientProxy.class), mock(CallbackContext.class),
                model, logger, proxyClient, null);
        progressEvent = ProgressEvent.defaultSuccessHandler(null);
    }

    @Test
    public void testDelete() {
        try {
            testAddOns.delete(progressEvent);
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }

    @Test
    public void testPreDelete() {
        try {
            testAddOns.preDelete(progressEvent);
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }

    @Test
    public void testCreate() {
        try {
            testAddOns.create(progressEvent);
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }

    @Test
    public void testPreCreate() {
        try {
            testAddOns.preCreate(progressEvent);
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }
}
