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
import software.amazon.awssdk.services.lightsail.model.AttachDiskRequest;
import software.amazon.awssdk.services.lightsail.model.DetachDiskRequest;
import software.amazon.awssdk.services.lightsail.model.GetDiskRequest;
import software.amazon.awssdk.services.lightsail.model.GetDiskResponse;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceResponse;
import software.amazon.awssdk.services.lightsail.model.Instance;
import software.amazon.awssdk.services.lightsail.model.InstanceHardware;
import software.amazon.awssdk.services.lightsail.model.InstanceState;
import software.amazon.awssdk.services.lightsail.model.NotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.instance.AbstractTestBase;
import software.amazon.lightsail.instance.AddOn;
import software.amazon.lightsail.instance.AutoSnapshotAddOn;
import software.amazon.lightsail.instance.Hardware;
import software.amazon.lightsail.instance.ResourceModel;
import software.amazon.lightsail.instance.State;
import software.amazon.lightsail.instance.helpers.resource.Disk;

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

class DiskTest {

    private Disk testDisk;

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
                .addOns(ImmutableList.of(AddOn.builder()
                        .addOnType(AddOnType.AUTO_SNAPSHOT.toString())
                        .status("Enabled")
                        .autoSnapshotAddOnRequest(AutoSnapshotAddOn.builder().build())
                        .build()))
                .hardware(Hardware.builder()
                        .disks(ImmutableSet.of(
                                software.amazon.lightsail.instance.Disk.builder().diskName("disk1")
                                        .isSystemDisk(false)
                                        .path("abc")
                                        .build(),
                                software.amazon.lightsail.instance.Disk.builder().diskName("disk2")
                                        .isSystemDisk(false)
                                        .path("abc")
                                        .build(),
                                software.amazon.lightsail.instance.Disk.builder().diskName("disk3")
                                        .isSystemDisk(false)
                                        .path("abc")
                                        .build(),
                                software.amazon.lightsail.instance.Disk.builder().diskName("disk4")
                                        .isSystemDisk(false)
                                        .path("abc")
                                        .build()
                        ))
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
        testDisk = new Disk(model, logger, proxyClient, resourceModelRequest);
    }

    @Test
    public void testUpdate() {
        try {
            testDisk.update(GetInstanceRequest.builder().build());
        } catch (final UnsupportedOperationException e) {
            // Test passed
        }
    }

    @Test
    public void attachTest() {
                val basicResponse = GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .hardware(InstanceHardware.builder()
                                .disks(ImmutableSet.of(
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("systemDisk")
                                                .isSystemDisk(true)
                                                .path("123")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk5")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk1")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk2")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk6")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .build()
                                ))
                                .build())
                        .build()).build();

        val attachDoneOne = GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .hardware(InstanceHardware.builder()
                                .disks(ImmutableSet.of(
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("systemDisk")
                                                .isSystemDisk(true)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk1")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk2")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk3")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build()
                                ))
                                .build())
                        .build()).build();

        val attachDone = GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .hardware(InstanceHardware.builder()
                                .disks(ImmutableSet.of(
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("systemDisk")
                                                .isSystemDisk(true)
                                                .path("123")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk1")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk2")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk3")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk4")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .build()
                                ))
                                .build())
                        .build()).build();

                when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenReturn(GetDiskResponse
                        .builder()
                        .disk(software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                .isAttached(false)
                                .attachmentState("detached")
                                .state("available")
                                .attachedTo(null)
                                .build())
                        .build());

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(basicResponse)
                .thenReturn(attachDoneOne)
                .thenReturn(attachDone);

        testDisk.attachDisks();

        verify(sdkClient, times(2)).attachDisk(any(AttachDiskRequest.class));
    }

    @Test
    public void testDetach() {
        val basicResponse = GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .hardware(InstanceHardware.builder()
                                .disks(ImmutableSet.of(
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("systemDisk")
                                                .isSystemDisk(true)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk5")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk1")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk2")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk6")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build()
                                ))
                                .build())
                        .build()).build();

                val detachDone = GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .hardware(InstanceHardware.builder()
                                .disks(ImmutableSet.of(
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("systemDisk")
                                                .isSystemDisk(true)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk1")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk2")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build()
                                ))
                                .build())
                        .build()).build();

        val detachDoneOne = GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .hardware(InstanceHardware.builder()
                                .disks(ImmutableSet.of(
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("systemDisk")
                                                .isSystemDisk(true)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk1")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk2")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk6")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build()
                                ))
                                .build())
                        .build()).build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(basicResponse)
                .thenReturn(detachDoneOne)
                .thenReturn(detachDone);

        testDisk.detachDisks();
        verify(sdkClient, times(2)).detachDisk(any(DetachDiskRequest.class));
    }

    @Test
    public void testUnSupportedMethods() {
        try {
            testDisk.create(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testDisk.delete(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testDisk.update(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testDisk.isStabilizedUpdate();
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testDisk.isStabilizedDelete();
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testDisk.isSafeExceptionCreateOrUpdate(NotFoundException.builder().build());
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testDisk.isSafeExceptionDelete(NotFoundException.builder().build());
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }
}
