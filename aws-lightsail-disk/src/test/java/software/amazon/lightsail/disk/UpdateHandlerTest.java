package software.amazon.lightsail.disk;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.val;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(DiskState.AVAILABLE.name())
                .addOns(ImmutableList.of(AddOn.builder()
                        .addOnType(AddOnType.AUTO_SNAPSHOT.toString())
                        .status("Enabled")
                        .autoSnapshotAddOnRequest(AutoSnapshotAddOn.builder().build())
                        .build()))
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(sdkClient.getDisk(any(GetDiskRequest.class))).thenReturn(GetDiskResponse.builder()
                .disk(Disk.builder().state(DiskState.AVAILABLE)
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .build()).build());

        when(sdkClient.enableAddOn(any(EnableAddOnRequest.class)))
                .thenReturn(EnableAddOnResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        val desiredResponse = request.getDesiredResourceState();
        desiredResponse.setPath("");
        desiredResponse.setAttachedTo("");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(desiredResponse);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SimpleSuccess_DisableAddOn() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(DiskState.AVAILABLE.name())
                .addOns(ImmutableList.of(AddOn.builder()
                        .addOnType(AddOnType.AUTO_SNAPSHOT.toString())
                        .status("Disabled")
                        .autoSnapshotAddOnRequest(AutoSnapshotAddOn.builder().build())
                        .build()))
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(sdkClient.getDisk(any(GetDiskRequest.class))).thenReturn(GetDiskResponse.builder()
                .disk(Disk.builder().state(DiskState.AVAILABLE)
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Disabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .build()).build());

        when(sdkClient.disableAddOn(any(DisableAddOnRequest.class)))
                .thenReturn(DisableAddOnResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        val desiredResponse = request.getDesiredResourceState();
        desiredResponse.setPath("");
        desiredResponse.setAttachedTo("");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(desiredResponse);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_errorEnableAddOn() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(DiskState.AVAILABLE.name())
                .addOns(ImmutableList.of(AddOn.builder()
                        .addOnType(AddOnType.AUTO_SNAPSHOT.toString())
                        .status("Enabled")
                        .autoSnapshotAddOnRequest(AutoSnapshotAddOn.builder().build())
                        .build()))
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(sdkClient.getDisk(any(GetDiskRequest.class))).thenReturn(GetDiskResponse.builder()
                .disk(Disk.builder().state(DiskState.AVAILABLE)
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
    public void handleRequest_SimpleSuccess_DisableAddOnAlreadyDisabled() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(DiskState.AVAILABLE.name())
                .addOns(ImmutableList.of(AddOn.builder()
                        .addOnType(AddOnType.AUTO_SNAPSHOT.toString())
                        .status("Disabled")
                        .autoSnapshotAddOnRequest(AutoSnapshotAddOn.builder().build())
                        .build()))
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(sdkClient.getDisk(any(GetDiskRequest.class))).thenReturn(GetDiskResponse.builder()
                .disk(Disk.builder().state(DiskState.AVAILABLE)
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Disabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .build()).build());

        doThrow(InvalidInputException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorMessage("The addOn is already in requested state")
                        .build())
                .build()).when(sdkClient).disableAddOn(any(DisableAddOnRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        val desiredResponse = request.getDesiredResourceState();
        desiredResponse.setPath("");
        desiredResponse.setAttachedTo("");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(desiredResponse);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SimpleSuccess_withTags() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(DiskState.AVAILABLE.name())
                .addOns(ImmutableList.of(AddOn.builder()
                        .addOnType(AddOnType.AUTO_SNAPSHOT.toString())
                        .status("Enabled")
                        .autoSnapshotAddOnRequest(AutoSnapshotAddOn.builder().build())
                        .build()))
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

        when(sdkClient.getDisk(any(GetDiskRequest.class))).thenReturn(GetDiskResponse.builder()
                .disk(Disk.builder().state(DiskState.AVAILABLE)
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Enabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
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

        when(sdkClient.enableAddOn(any(EnableAddOnRequest.class)))
                .thenReturn(EnableAddOnResponse.builder().build());

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
}
