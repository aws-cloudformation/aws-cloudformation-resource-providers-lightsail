package software.amazon.lightsail.disk.helpers.resource;

import com.google.common.collect.ImmutableList;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.AddOnType;
import software.amazon.awssdk.services.lightsail.model.CreateDiskRequest;
import software.amazon.awssdk.services.lightsail.model.CreateDiskResponse;
import software.amazon.awssdk.services.lightsail.model.DeleteDiskRequest;
import software.amazon.awssdk.services.lightsail.model.DeleteDiskResponse;
import software.amazon.awssdk.services.lightsail.model.GetDiskRequest;
import software.amazon.awssdk.services.lightsail.model.GetDiskResponse;
import software.amazon.awssdk.services.lightsail.model.NotFoundException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.lightsail.disk.AbstractTestBase.MOCK_CREDENTIALS;

class DiskTest {

    private Disk testDisk;

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
        testDisk = new Disk(model, logger, proxyClient, null);
    }

    @AfterEach
    public void tear_down() {
    }

    @Test
    public void testCreate() {
        when(sdkClient.createDisk(any(CreateDiskRequest.class)))
                .thenReturn(CreateDiskResponse.builder().build());
        val result = testDisk.create(CreateDiskRequest.builder().build());
        assertThat(result).isNotNull();
    }

    @Test
    public void testRead() {
        when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenReturn(GetDiskResponse.builder().build());
        val result = testDisk.read(GetDiskRequest.builder().build());
        assertThat(result).isNotNull();
    }

    @Test
    public void testDelete() {
        when(sdkClient.deleteDisk(any(DeleteDiskRequest.class)))
                .thenReturn(DeleteDiskResponse.builder().build());
        val result = testDisk.delete(DeleteDiskRequest.builder().build());
        assertThat(result).isNotNull();
    }

    @Test
    public void testIsStabilizedCreateOrUpdate() {
        when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenReturn(GetDiskResponse.builder()
                        .disk(software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                .state("available").build())
                        .build());
        val result = testDisk.isStabilizedUpdate();
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedCreateOrUpdatePending() {
        when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenReturn(GetDiskResponse.builder()
                        .disk(software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                .build())
                        .build());
        val result = testDisk.isStabilizedUpdate();
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedDelete() {
        doThrow(NotFoundException.class).when(sdkClient).getDisk(any(GetDiskRequest.class));
        val result = testDisk.isStabilizedDelete();
        assertThat(result).isTrue();
    }


    @Test
    public void testIsStabilizedDeleteNotStablized() {
        when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenReturn(GetDiskResponse.builder()
                        .disk(software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                .build())
                        .build());
        val result = testDisk.isStabilizedDelete();
        assertThat(result).isFalse();
    }

    @Test
    public void testIsDiskFree() {
        when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenReturn(GetDiskResponse.builder()
                        .disk(software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                .state("available")
                                .attachmentState("detached")
                                .build())
                        .build());
        val result = testDisk.isDiskFree();
        assertThat(result).isTrue();
    }

    @Test
    public void testIsDiskFreeNotFree() {
        when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenReturn(GetDiskResponse.builder()
                        .disk(software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                .attachmentState("detached")
                                .build())
                        .build());
        val result = testDisk.isDiskFree();
        assertThat(result).isFalse();
    }

}
