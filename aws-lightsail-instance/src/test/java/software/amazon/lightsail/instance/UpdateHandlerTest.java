package software.amazon.lightsail.instance;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.AddOnType;
import software.amazon.awssdk.services.lightsail.model.AttachDiskRequest;
import software.amazon.awssdk.services.lightsail.model.DetachDiskRequest;
import software.amazon.awssdk.services.lightsail.model.DisableAddOnRequest;
import software.amazon.awssdk.services.lightsail.model.DisableAddOnResponse;
import software.amazon.awssdk.services.lightsail.model.EnableAddOnRequest;
import software.amazon.awssdk.services.lightsail.model.EnableAddOnResponse;
import software.amazon.awssdk.services.lightsail.model.GetDiskRequest;
import software.amazon.awssdk.services.lightsail.model.GetDiskResponse;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceResponse;
import software.amazon.awssdk.services.lightsail.model.Instance;
import software.amazon.awssdk.services.lightsail.model.InstanceHardware;
import software.amazon.awssdk.services.lightsail.model.InstanceNetworking;
import software.amazon.awssdk.services.lightsail.model.InstancePortInfo;
import software.amazon.awssdk.services.lightsail.model.InstanceState;
import software.amazon.awssdk.services.lightsail.model.InvalidInputException;
import software.amazon.awssdk.services.lightsail.model.MonthlyTransfer;
import software.amazon.awssdk.services.lightsail.model.PutInstancePublicPortsRequest;
import software.amazon.awssdk.services.lightsail.model.PutInstancePublicPortsResponse;
import software.amazon.awssdk.services.lightsail.model.TagResourceRequest;
import software.amazon.awssdk.services.lightsail.model.UntagResourceRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_attachDiskDetachDisk() {
        final UpdateHandler handler = new UpdateHandler();

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
                                Disk.builder().diskName("disk1")
                                        .isSystemDisk(false)
                                        .path("abc")
                                        .build(),
                                Disk.builder().diskName("disk2")
                                        .isSystemDisk(false)
                                        .path("abc")
                                        .build(),
                                Disk.builder().diskName("disk3")
                                        .isSystemDisk(false)
                                        .path("abc")
                                        .build(),
                                Disk.builder().diskName("disk4")
                                        .isSystemDisk(false)
                                        .path("abc")
                                        .build()
                        ))
                        .build())
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

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
                                                .name("disk3")
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
                                                .attachmentState("attached")
                                                .build(),
                                        software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                                .name("disk4")
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
                                                .name("disk3")
                                                .isSystemDisk(false)
                                                .path("123")
                                                .attachmentState("attached")
                                                .build()
                                ))
                                .build())
                        .build()).build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(detachDone)
                .thenReturn(detachDone)
                .thenReturn(detachDone)
                .thenReturn(detachDone)
                .thenReturn(detachDone)
                .thenReturn(attachDone);

        when(sdkClient.enableAddOn(any(EnableAddOnRequest.class)))
                .thenReturn(EnableAddOnResponse.builder().build());

        when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenReturn(GetDiskResponse.builder()
                        .disk(software.amazon.awssdk.services.lightsail.model.Disk.builder()
                                .attachmentState("detached")
                                .state("available")
                                .build())
                        .build());
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(sdkClient, times(1)).attachDisk(any(AttachDiskRequest.class));
        verify(sdkClient, times(1)).detachDisk(any(DetachDiskRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final UpdateHandler handler = new UpdateHandler();

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
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class))).thenReturn(GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .build()).build());

        when(sdkClient.enableAddOn(any(EnableAddOnRequest.class)))
                .thenReturn(EnableAddOnResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SimpleSuccess_withTags() {
        final UpdateHandler handler = new UpdateHandler();

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
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class))).thenReturn(GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .build()).build());

        when(sdkClient.enableAddOn(any(EnableAddOnRequest.class)))
                .thenReturn(EnableAddOnResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SimpleSuccess_DisableAddOn_Networking() {
        final UpdateHandler handler = new UpdateHandler();

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
                                        .accessDirection("one")
                                        .fromPort(20)
                                        .toPort(40)
                                        .cidrs(ImmutableList.of("1.2.2.4"))
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

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class))).thenReturn(GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Disabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .networking(InstanceNetworking.builder()
                                .ports(ImmutableList.of(
                                        InstancePortInfo
                                                .builder()
                                                .cidrs(ImmutableList.of("1.2.2.4"))
                                                .cidrListAliases(ImmutableList.of("1.2.2.2"))
                                                .ipv6Cidrs(ImmutableList.of("2.2.24.2.3.2323.232323:/dsds"))
                                                .fromPort(20)
                                                .toPort(40)
                                                .accessDirection("one")
                                                .accessFrom("1")
                                                .build()))
                                .monthlyTransfer(MonthlyTransfer.builder().gbPerMonthAllocated(20).build())
                                .build())
                        .tags(ImmutableSet.of(
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key1").value("value1").build(),
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key2").value("value5").build(), // different value than what is there.
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key6").value("value6").build(), // new Key new Value
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key6").value("value2").build(), // new Key old Value.
                                software.amazon.awssdk.services.lightsail.model.Tag.builder()
                                        .key("key4").build()))
                        .build()).build());

        when(sdkClient.disableAddOn(any(DisableAddOnRequest.class)))
                .thenReturn(DisableAddOnResponse.builder().build());

        when(sdkClient.putInstancePublicPorts(any(PutInstancePublicPortsRequest.class)))
                .thenReturn(PutInstancePublicPortsResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ArgumentCaptor<TagResourceRequest> captor1 = ArgumentCaptor.forClass(TagResourceRequest.class);
        ArgumentCaptor<UntagResourceRequest> captor2 = ArgumentCaptor.forClass(UntagResourceRequest.class);
        verify(sdkClient, times(1)).tagResource(captor1.capture());
        verify(sdkClient, times(1)).untagResource(captor2.capture());
        assertNotNull(captor1.getValue());
        assertNotNull(captor2.getValue());
        assertNotNull(captor1.getValue().tags());
        assertNotNull(captor2.getValue().tagKeys());
        assertEquals(5, captor1.getValue().tags().size());
        assertEquals(3, captor2.getValue().tagKeys().size());
    }

    @Test
    public void handleRequest_errorEnableAddOn() {
        final UpdateHandler handler = new UpdateHandler();

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
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class))).thenReturn(GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .build()).build());

        doThrow(AwsServiceException
                .builder().build())
                .when(sdkClient).enableAddOn(any(EnableAddOnRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void handleRequest_SimpleSuccess_DisableAddOnAlreadyDisabled_Networking() {
        final UpdateHandler handler = new UpdateHandler();

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
                                        .accessDirection("one")
                                        .fromPort(20)
                                        .toPort(40)
                                        .cidrs(ImmutableList.of("1.2.2.4"))
                                        .cidrListAliases(ImmutableList.of("1.2.2.2"))
                                        .build()))
                        .monthlyTransfer(software.amazon.lightsail.instance.MonthlyTransfer.builder()
                                .gbPerMonthAllocated("20").build())
                        .build())
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class))).thenReturn(GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Disabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .networking(InstanceNetworking.builder()
                                .ports(ImmutableList.of(
                                        InstancePortInfo
                                                .builder()
                                                .cidrs(ImmutableList.of("1.2.2.4"))
                                                .cidrListAliases(ImmutableList.of("1.2.2.2"))
                                                .ipv6Cidrs(ImmutableList.of("2.2.24.2.3.2323.232323:/dsds"))
                                                .fromPort(20)
                                                .toPort(40)
                                                .accessDirection("one")
                                                .accessFrom("1")
                                                .build()))
                                .monthlyTransfer(MonthlyTransfer.builder().gbPerMonthAllocated(20).build())
                                .build())
                        .build()).build());

        doThrow(InvalidInputException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorMessage("The addOn is already in requested state")
                        .build())
                .build()).when(sdkClient).disableAddOn(any(DisableAddOnRequest.class));

        when(sdkClient.putInstancePublicPorts(any(PutInstancePublicPortsRequest.class)))
                .thenReturn(PutInstancePublicPortsResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SimpleSuccess_DisableAddOnAlreadyDisabled_NetworkingError() {
        final UpdateHandler handler = new UpdateHandler();

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
                                        .accessDirection("one")
                                        .fromPort(20)
                                        .toPort(40)
                                        .cidrListAliases(ImmutableList.of("1.2.2.2"))
                                        .build()))
                        .monthlyTransfer(software.amazon.lightsail.instance.MonthlyTransfer.builder()
                                .gbPerMonthAllocated("20").build())
                        .build())
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class))).thenReturn(GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Disabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .networking(InstanceNetworking.builder()
                                .ports(ImmutableList.of(
                                        InstancePortInfo
                                                .builder()
                                                .cidrListAliases(ImmutableList.of("1.2.2.2"))
                                                .ipv6Cidrs(ImmutableList.of("2.2.24.2.3.2323.232323:/dsds"))
                                                .fromPort(20)
                                                .toPort(40)
                                                .accessDirection("one")
                                                .accessFrom("1")
                                                .build()))
                                .monthlyTransfer(MonthlyTransfer.builder().gbPerMonthAllocated(20).build())
                                .build())
                        .build()).build());

        doThrow(InvalidInputException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorMessage("The addOn is already in requested state")
                        .build())
                .build()).when(sdkClient).disableAddOn(any(DisableAddOnRequest.class));

        doThrow(InvalidInputException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorMessage("Invalid Input")
                        .build())
                .build()).when(sdkClient).putInstancePublicPorts(any(PutInstancePublicPortsRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }
}
