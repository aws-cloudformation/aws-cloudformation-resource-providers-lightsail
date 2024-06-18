package software.amazon.lightsail.instance;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import com.amazonaws.regions.Regions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.val;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.AddOnType;
import software.amazon.awssdk.services.lightsail.model.AvailabilityZone;
import software.amazon.awssdk.services.lightsail.model.CreateInstancesRequest;
import software.amazon.awssdk.services.lightsail.model.CreateInstancesResponse;
import software.amazon.awssdk.services.lightsail.model.DisableAddOnRequest;
import software.amazon.awssdk.services.lightsail.model.DisableAddOnResponse;
import software.amazon.awssdk.services.lightsail.model.GetInstanceRequest;
import software.amazon.awssdk.services.lightsail.model.GetInstanceResponse;
import software.amazon.awssdk.services.lightsail.model.GetRegionsRequest;
import software.amazon.awssdk.services.lightsail.model.GetRegionsResponse;
import software.amazon.awssdk.services.lightsail.model.Instance;
import software.amazon.awssdk.services.lightsail.model.InstanceNetworking;
import software.amazon.awssdk.services.lightsail.model.InstancePortInfo;
import software.amazon.awssdk.services.lightsail.model.InstanceState;
import software.amazon.awssdk.services.lightsail.model.InvalidInputException;
import software.amazon.awssdk.services.lightsail.model.NotFoundException;
import software.amazon.awssdk.services.lightsail.model.PutInstancePublicPortsRequest;
import software.amazon.awssdk.services.lightsail.model.PutInstancePublicPortsResponse;
import software.amazon.awssdk.services.lightsail.model.Region;
import software.amazon.awssdk.services.lightsail.model.ResourceLocation;
import software.amazon.awssdk.services.lightsail.model.ServiceException;
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
import software.amazon.lightsail.instance.helpers.handler.AddOnsHandler;
import software.amazon.lightsail.instance.helpers.handler.DiskHandler;
import software.amazon.lightsail.instance.helpers.handler.InstanceHandler;
import software.amazon.lightsail.instance.helpers.handler.NetworkHandler;
import software.amazon.lightsail.instance.helpers.handler.TagsHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
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
        verify(sdkClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(State.builder().name("Running").build())
                .location(Location.builder().availabilityZone("us-west-2a").build())
                .ipv6Addresses(Collections.emptyList())
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .region("us-west-2")
                .build();

        GetInstanceResponse getInstanceResponse = GetInstanceResponse.builder()
                .instance(Instance.builder()
                        .location(ResourceLocation.builder().availabilityZone("us-west-2a").build())
                        .state(InstanceState.builder()
                                .name("Running").build()).build()).build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build())
                .thenReturn(getInstanceResponse)
                .thenReturn(getInstanceResponse);

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
                                                            .build()).build())).build());

        when(sdkClient.createInstances(any(CreateInstancesRequest.class)))
                .thenReturn(CreateInstancesResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        model.setAvailabilityZone("us-west-2a");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SimpleSuccess_updateFlow() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                 .addOns(new ArrayList<>())
                .state(State.builder()
                        .name("Running")
                        .build())
                .availabilityZone("us-west-2a")
                .ipv6Addresses(Collections.emptyList())
                .location(Location.builder().availabilityZone("us-west-2a").build())
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
                        .monthlyTransfer(MonthlyTransfer.builder()
                                .gbPerMonthAllocated("20").build())
                        .build())
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .region("us-west-2")
                .build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build())
                .thenReturn(GetInstanceResponse.builder()
                .instance(Instance.builder().state(InstanceState
                        .builder()
                        .name("Running")
                        .build())
                        .location(ResourceLocation.builder().availabilityZone("us-west-2a").build())
                        .addOns(ImmutableList.of(software.amazon.awssdk.services.lightsail.model.AddOn.builder()
                                .status("Disabled")
                                .name(AddOnType.AUTO_SNAPSHOT.toString())
                                .build()))
                        .networking(InstanceNetworking.builder()
                                .ports(ImmutableList.of(
                                        InstancePortInfo
                                                .builder()
                                                .cidrListAliases(ImmutableList.of("1.2.2.2"))
                                                .cidrs(ImmutableList.of("1.2.2.232"))
                                                .ipv6Cidrs(ImmutableList.of("2.2.24.2.3.2323.232323:/dsds"))
                                                .fromPort(20)
                                                .toPort(40)
                                                .accessDirection("one")
                                                .accessFrom("1")
                                                .build()))
                                .monthlyTransfer(software.amazon.awssdk.services.lightsail.model.MonthlyTransfer
                                        .builder().gbPerMonthAllocated(20).build())
                                .build())
                        .build()).build());

        when(sdkClient.createInstances(any(CreateInstancesRequest.class)))
                .thenReturn(CreateInstancesResponse.builder().build());

        when(sdkClient.disableAddOn(any(DisableAddOnRequest.class)))
                .thenReturn(DisableAddOnResponse.builder().build());

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
    public void handleRequest_ErrorInCreate() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .state(State.builder().name("Running").build())
                .availabilityZone("us-west-2a")
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .region("us-west-2")
                .build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build())
                .thenReturn(GetInstanceResponse.builder()
                .instance(Instance.builder()
                        .location(ResourceLocation.builder().availabilityZone("us-west-2a").build())
                        .state(InstanceState.builder().name("Running").build())
                        .build()).build());

        doThrow(ServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("ServiceException").build())
                .build()).when(sdkClient).createInstances(any(CreateInstancesRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_ErrorInCreateThrottling() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .addOns(new ArrayList<>())
                .availabilityZone("us-west-2a")
                .state(State.builder().name("Running").build())
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .region("us-west-2")
                .build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build())
                .thenReturn(GetInstanceResponse.builder()
                .instance(Instance.builder()
                        .location(ResourceLocation.builder().availabilityZone("us-west-2a").build())
                        .state(InstanceState.builder().name("Running").build()).build()).build());

        doThrow(ServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("ThrottlingException").build())
                .build()).when(sdkClient).createInstances(any(CreateInstancesRequest.class));

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
                .availabilityZone("us-west-2a")
                .state(State.builder().name("Running").build())
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .region("us-west-2")
                .build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build())
                .thenReturn(GetInstanceResponse.builder()
                .instance(Instance.builder()
                        .location(ResourceLocation.builder().availabilityZone("us-west-2a").build())
                        .state(InstanceState.builder().name("Running").build()).build()).build());

        doThrow(ServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDeniedException").build())
                .build()).when(sdkClient).createInstances(any(CreateInstancesRequest.class));

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
                .availabilityZone("us-west-2a")
                .state(State.builder().name("Running").build())
                .tags(new HashSet<>()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .region("us-west-2")
                .build();

        when(sdkClient.getInstance(any(GetInstanceRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build())
                .thenReturn(GetInstanceResponse.builder()
                .instance(Instance.builder()
                        .location(ResourceLocation.builder().availabilityZone("us-west-2a").build())
                        .state(InstanceState.builder().name("Running").build()).build()).build());

        doThrow(RuntimeException.class).when(sdkClient).createInstances(any(CreateInstancesRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

}
