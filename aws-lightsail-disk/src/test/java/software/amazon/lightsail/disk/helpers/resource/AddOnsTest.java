package software.amazon.lightsail.disk.helpers.resource;

import com.google.common.collect.ImmutableList;
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
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.lightsail.disk.AbstractTestBase;
import software.amazon.lightsail.disk.AddOn;
import software.amazon.lightsail.disk.AutoSnapshotAddOn;
import software.amazon.lightsail.disk.ResourceModel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.amazon.lightsail.disk.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
class AddOnsTest {


    private AddOns testAddOns;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    private LightsailClient sdkClient;

    @BeforeEach
    public void setup() {

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state("Available")
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
        testAddOns = new AddOns(model, logger, proxyClient, null);
    }

    @AfterEach
    public void tear_down() {
    }


    @Test
    public void testUpdate_disable() {
        testAddOns.update(any(AwsRequest.class));
        verify(sdkClient, times(1)).disableAddOn(any(DisableAddOnRequest.class));
    }

    @Test
    public void testUpdate_enable() {
        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state("Available")
                .addOns(ImmutableList.of(AddOn.builder()
                        .addOnType(AddOnType.AUTO_SNAPSHOT.toString())
                        .status("Enabled")
                        .autoSnapshotAddOnRequest(AutoSnapshotAddOn.builder().build())
                        .build()))
                .tags(new HashSet<>()).build();
        val logger = mock(Logger.class);
        sdkClient = mock(LightsailClient.class);
        ProxyClient<LightsailClient> proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        testAddOns = new AddOns(model, logger, proxyClient, null);
        testAddOns.update(any(AwsRequest.class));
        verify(sdkClient, times(1)).enableAddOn(any(EnableAddOnRequest.class));
    }
}
