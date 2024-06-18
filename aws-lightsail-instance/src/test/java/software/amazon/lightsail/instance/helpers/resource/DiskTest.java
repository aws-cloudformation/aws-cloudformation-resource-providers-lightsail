package software.amazon.lightsail.instance.helpers.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.DetachDiskRequest;
import software.amazon.awssdk.services.lightsail.model.OperationFailureException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.instance.*;
import software.amazon.lightsail.instance.Disk;
import software.amazon.lightsail.instance.Tag;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static software.amazon.lightsail.instance.AbstractTestBase.MOCK_CREDENTIALS;

class DiskTest {

    private software.amazon.lightsail.instance.helpers.resource.Disk diskTest;

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
                .tags(ImmutableSet.of(
                        Tag.builder().key("key1").value("value1").build(),
                        Tag.builder().key("key2").value("value2").build(),
                        Tag.builder().key("key3").value("value3").build(),
                        Tag.builder().key("key1").value("value2").build(),
                        Tag.builder().key("key4").value("value3").build(),
                        Tag.builder().key("key5").build())
                ).build();
        val logger = mock(Logger.class);
        sdkClient = mock(LightsailClient.class);
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        ProxyClient<LightsailClient> proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        diskTest = new software.amazon.lightsail.instance.helpers.resource.Disk(model, logger,
                proxyClient, resourceModelRequest);
    }

    @Test
    public void testDisksNeedAttachment() {
        software.amazon.lightsail.instance.Disk disk1 = software.amazon.lightsail.instance.Disk.builder()
                .diskName("disk1")
                .attachmentState("Attached")
                .build();
        val disk2 = software.amazon.lightsail.instance.Disk.builder()
                .diskName("disk2")
                .attachmentState("Detached")
                .build();
        val disk3 = software.amazon.lightsail.instance.Disk.builder()
                .diskName("disk3")
                .attachmentState("Detaching")
                .build();
        val disk4 = software.amazon.lightsail.instance.Disk.builder()
                .diskName("disk4")
                .attachmentState("Pending")
                .build();
        val disk5 = software.amazon.lightsail.instance.Disk.builder()
                .diskName("disk5")
                .attachmentState("Attached")
                .build();
        val disk6 = software.amazon.lightsail.instance.Disk.builder()
                .diskName("disk6")
                .attachmentState("Attaching")
                .build();
        val disk7 = software.amazon.lightsail.instance.Disk.builder()
                .diskName("disk7")
                .build();
        val disk8 = software.amazon.lightsail.instance.Disk.builder()
                .diskName("disk8")
                .build();

        Set<software.amazon.lightsail.instance.Disk> currDisks = ImmutableSet.of(disk1, disk2, disk3, disk4, disk5, disk6);
        Set<software.amazon.lightsail.instance.Disk> desiredDisks = ImmutableSet.of(disk1, disk5,
                disk2, disk3, disk4, disk7, disk8);
        val result = diskTest.getDisksNeedAttachment(currDisks, desiredDisks);
        result.forEach(disk -> System.out.println(disk.getDiskName()));
        assertEquals(2, result.size());
    }

    @Test
    public void testDetachSuccessOnException() {
        when(sdkClient.detachDisk(any(DetachDiskRequest.class)))
                .thenThrow(OperationFailureException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder()
                                .errorMessage("You can't detach this disk right now. The state of this disk is: available").build())
                        .build());
        Collection<Disk> disksNeedDetachment = new ArrayList<Disk>() {{
            add(Disk.builder().diskName("disk1").build());
            add(Disk.builder().diskName("disk2").build());
        }};
        diskTest.detach(disksNeedDetachment);
        verify(sdkClient, times(1)).detachDisk(any(DetachDiskRequest.class));
    }

    @Test
    public void testDetachFailOnException() {
        when(sdkClient.detachDisk(any(DetachDiskRequest.class)))
                .thenThrow(OperationFailureException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder()
                                .errorMessage("You can only detach the disk when the target instance is stopped. The current state of your instance is: running").build())
                        .build());
        Collection<Disk> disksNeedDetachment = new ArrayList<Disk>() {{
            add(Disk.builder().diskName("disk1").build());
            add(Disk.builder().diskName("disk2").build());
        }};
        try {
            diskTest.detach(disksNeedDetachment);
            fail();
        } catch (OperationFailureException e) {
            // pass the exception
        }
    }
}
