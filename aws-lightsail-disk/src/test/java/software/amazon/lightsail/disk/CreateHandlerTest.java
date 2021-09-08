package software.amazon.lightsail.disk;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;

import com.amazonaws.regions.Regions;
import com.google.common.collect.ImmutableList;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

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
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(DiskState.AVAILABLE.name())
                .location(Location.builder().availabilityZone("us-west-2a").build())
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request1 = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .region("us-west-2")
                .build();

        when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build())
                .thenReturn(GetDiskResponse.builder()
                .disk(Disk.builder()
                        .state("available")
                        .location(ResourceLocation.builder().availabilityZone("us-west-2a").build())
                        .build()).build());

        when(sdkClient.getRegions(any(GetRegionsRequest.class))).thenReturn(
                GetRegionsResponse
                        .builder()
                        .regions(ImmutableList.of(Region.builder()
                                .name(Regions.US_WEST_2.getName())
                                .availabilityZones(
                                        AvailabilityZone.builder()
                                                .zoneName("us-west-2a")
                                        .build(),
                                        AvailabilityZone.builder()
                                                .zoneName("us-west-2b")
                                                .build(),
                                        AvailabilityZone.builder()
                                                .zoneName("us-west-2c")
                                                .build(),
                                        AvailabilityZone.builder()
                                                .zoneName("us-west-2d")
                                                .build())
                                .build()))
                        .build()
        );

        when(sdkClient.createDisk(any(CreateDiskRequest.class)))
                .thenReturn(CreateDiskResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request1, new CallbackContext(), proxyClient, logger);

        model.setAvailabilityZone("us-west-2a");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request1.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ErrorInCreate() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state("available")
                .availabilityZone("us-west-2a")
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                        .region("us-west-2")
                .build();


        when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build())
                .thenReturn(GetDiskResponse.builder()
                .disk(Disk.builder()
                        .state("available")
                        .location(ResourceLocation.builder().availabilityZone("us-west-2a").build())
                        .build()).build());

        doThrow(ServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("ServiceException").build())
                .build()).when(sdkClient).createDisk(any(CreateDiskRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void handleRequest_ErrorInCreateThrottling() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state("available")
                .availabilityZone("us-west-2a")
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .region("us-west-2")
                .build();

        when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build())
                .thenReturn(GetDiskResponse.builder()
                .disk(Disk.builder()
                        .state(DiskState.AVAILABLE)
                        .location(ResourceLocation.builder().availabilityZone("us-west-2a").build())
                .build()).build());

        doThrow(ServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("ThrottlingException").build())
                .build()).when(sdkClient)
                .createDisk(any(CreateDiskRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void handleRequest_ErrorInCreateAuth() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(DiskState.AVAILABLE.name())
                .availabilityZone("us-west-2a")
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .region("us-west-2")
                .build();

        when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build())
                .thenReturn(
                GetDiskResponse.builder()
                        .disk(Disk.builder()
                                .location(ResourceLocation.builder().availabilityZone("us-west-2a").build())
                                .state(DiskState.AVAILABLE).build()).build());

        doThrow(ServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDeniedException").build())
                .build()).when(sdkClient).createDisk(any(CreateDiskRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }

    @Test
    public void handleRequest_ErrorRandom() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(DiskState.AVAILABLE.name())
                .availabilityZone("us-west-2a")
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .region("us-west-2")
                .build();

        when(sdkClient.getDisk(any(GetDiskRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build())
                .thenReturn(GetDiskResponse.builder()
                .disk(Disk.builder()
                        .location(ResourceLocation.builder().availabilityZone("us-west-2a").build())
                        .state(DiskState.AVAILABLE).build()).build());

        doThrow(RuntimeException.class).when(sdkClient)
                .createDisk(any(CreateDiskRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }
}
