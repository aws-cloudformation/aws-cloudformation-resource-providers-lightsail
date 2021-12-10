package software.amazon.lightsail.bucket.helpers.resource;

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
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.bucket.AbstractTestBase;
import software.amazon.lightsail.bucket.ResourceModel;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.bucket.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class BucketTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    private Logger logger;
    private Bucket testBucket;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        logger = mock(Logger.class);

        final ResourceModel model = ResourceModel.builder().bundleId("small_1_0")
                .resourcesReceivingAccess(new HashSet<>(Arrays.asList("resource1", "resource2"))).build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        testBucket = new Bucket(model, logger, proxyClient, resourceModelRequest);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testCreate() {
        when(sdkClient.createBucket(any(CreateBucketRequest.class)))
                .thenReturn(CreateBucketResponse.builder().build());
        val result = testBucket.create(CreateBucketRequest.builder().build());
        verify(sdkClient, times(1)).createBucket(any(CreateBucketRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testRead() {
        when(sdkClient.getBuckets(any(GetBucketsRequest.class)))
                .thenReturn(GetBucketsResponse.builder().build());
        val result = testBucket.read(GetBucketsRequest.builder().build());
        verify(sdkClient, times(1)).getBuckets(any(GetBucketsRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testReadAll() {
        when(sdkClient.getBuckets(any(GetBucketsRequest.class)))
                .thenReturn(GetBucketsResponse.builder().build());
        val result = testBucket.readAll(GetBucketsRequest.builder().build());
        verify(sdkClient, times(1)).getBuckets(any(GetBucketsRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testSetResourceAccess_allow() {
        when(sdkClient.setResourceAccessForBucket(any(SetResourceAccessForBucketRequest.class)))
                .thenReturn(SetResourceAccessForBucketResponse.builder().build());
        val result = testBucket.setResourceAccess("resource", true);

        ArgumentCaptor<SetResourceAccessForBucketRequest> captor = ArgumentCaptor.forClass(SetResourceAccessForBucketRequest.class);
        verify(sdkClient, times(1))
                .setResourceAccessForBucket(captor.capture());
        assertThat(result).isNotNull();
        assertThat(captor.getValue().access().name()).isEqualTo("ALLOW");
        assertThat(captor.getValue().resourceName()).isEqualTo("resource");
    }

    @Test
    public void testSetResourceAccess_deny() {
        when(sdkClient.setResourceAccessForBucket(any(SetResourceAccessForBucketRequest.class)))
                .thenReturn(SetResourceAccessForBucketResponse.builder().build());
        val result = testBucket.setResourceAccess("resource", false);

        ArgumentCaptor<SetResourceAccessForBucketRequest> captor = ArgumentCaptor.forClass(SetResourceAccessForBucketRequest.class);
        verify(sdkClient, times(1))
                .setResourceAccessForBucket(captor.capture());
        assertThat(result).isNotNull();
        assertThat(captor.getValue().access().name()).isEqualTo("DENY");
        assertThat(captor.getValue().resourceName()).isEqualTo("resource");
    }

    @Test
    public void testDetachInstances() {
        when(sdkClient.getBuckets(any(GetBucketsRequest.class)))
                .thenReturn(GetBucketsResponse.builder()
                                .buckets(Arrays.asList(software.amazon.awssdk.services.lightsail.model.Bucket.builder()
                                        .accessRules(AccessRules.builder().getObject("public").allowPublicOverrides(true).build())
                                        .readonlyAccessAccounts(new HashSet<>(Arrays.asList("1234567890")))
                                        .resourcesReceivingAccess(ResourceReceivingAccess.builder().name("resource3").resourceType("Instance").build())
                                        .objectVersioning("Enabled").build())).build());
        val result = testBucket.detachInstances(GetBucketsRequest.builder().build());
        verify(sdkClient, times(1)).getBuckets(any(GetBucketsRequest.class));
        verify(sdkClient, times(1)).setResourceAccessForBucket(any(SetResourceAccessForBucketRequest.class));
    }

    @Test
    public void testAttachInstances() {
        when(sdkClient.getBuckets(any(GetBucketsRequest.class)))
                .thenReturn(GetBucketsResponse.builder()
                        .buckets(Arrays.asList(software.amazon.awssdk.services.lightsail.model.Bucket.builder()
                                .accessRules(AccessRules.builder().getObject("public").allowPublicOverrides(true).build())
                                .readonlyAccessAccounts(new HashSet<>(Arrays.asList("1234567890")))
                                .resourcesReceivingAccess(ResourceReceivingAccess.builder().name("Resource3").resourceType("Instance").build())
                                .objectVersioning("Enabled").build())).build());
        val result = testBucket.attachInstances(GetBucketsRequest.builder().build());
        verify(sdkClient, times(1)).getBuckets(any(GetBucketsRequest.class));
        verify(sdkClient, times(2)).setResourceAccessForBucket(any(SetResourceAccessForBucketRequest.class));
    }

    @Test
    public void testDelete() {
        when(sdkClient.deleteBucket(any(DeleteBucketRequest.class)))
                .thenReturn(DeleteBucketResponse.builder().build());
        val result = testBucket.delete(DeleteBucketRequest.builder().build());
        verify(sdkClient, times(1)).deleteBucket(any(DeleteBucketRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testUpdate() {
        when(sdkClient.updateBucket(any(UpdateBucketRequest.class)))
                .thenReturn(UpdateBucketResponse.builder().build());
        val result = testBucket.update(UpdateBucketRequest.builder().build());
        verify(sdkClient, times(1)).updateBucket(any(UpdateBucketRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testUpdateBundle_noUpdateRequired() {
        when(sdkClient.getBuckets(any(GetBucketsRequest.class)))
                .thenReturn(GetBucketsResponse.builder()
                        .buckets(software.amazon.awssdk.services.lightsail.model.Bucket.builder()
                                .bundleId("small_1_0")
                                .build()).build());
        val result = testBucket.updateBundle(UpdateBucketBundleRequest.builder().build());
        verify(sdkClient, never()).updateBucketBundle(any(UpdateBucketBundleRequest.class));
        assertThat(result).isNull();
    }

    @Test
    public void testUpdateBundle_updateRequired() {
        when(sdkClient.getBuckets(any(GetBucketsRequest.class)))
                .thenReturn(GetBucketsResponse.builder()
                        .buckets(software.amazon.awssdk.services.lightsail.model.Bucket.builder()
                                .bundleId("large_1_0")
                                .build()).build());
        when(sdkClient.updateBucketBundle(any(UpdateBucketBundleRequest.class)))
                .thenReturn(UpdateBucketBundleResponse.builder().build());
        val result = testBucket.updateBundle(UpdateBucketBundleRequest.builder().build());
        verify(sdkClient, times(1)).updateBucketBundle(any(UpdateBucketBundleRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testIsStabilizedCreate_stabilized() {
        when(sdkClient.getBuckets(any(GetBucketsRequest.class)))
                .thenReturn(GetBucketsResponse.builder()
                        .buckets(software.amazon.awssdk.services.lightsail.model.Bucket.builder()
                                .state(BucketState.builder().code("OK").build())
                                .build()).build());
        val result = testBucket.isStabilizedCreate();
        verify(sdkClient, times(1)).getBuckets(any(GetBucketsRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedCreate_notStabilized() {
        when(sdkClient.getBuckets(any(GetBucketsRequest.class)))
                .thenReturn(GetBucketsResponse.builder()
                        .buckets(software.amazon.awssdk.services.lightsail.model.Bucket.builder()
                                .state(BucketState.builder().code("Unknown").build())
                                .build()).build());
        val result = testBucket.isStabilizedCreate();
        verify(sdkClient, times(1)).getBuckets(any(GetBucketsRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedUpdate_stabilized() {
        when(sdkClient.getBuckets(any(GetBucketsRequest.class)))
                .thenReturn(GetBucketsResponse.builder()
                        .buckets(software.amazon.awssdk.services.lightsail.model.Bucket.builder()
                                .state(BucketState.builder().code("OK").build())
                                .build()).build());
        val result = testBucket.isStabilizedUpdate();
        verify(sdkClient, times(1)).getBuckets(any(GetBucketsRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedUpdate_notStabilized() {
        when(sdkClient.getBuckets(any(GetBucketsRequest.class)))
                .thenReturn(GetBucketsResponse.builder()
                        .buckets(software.amazon.awssdk.services.lightsail.model.Bucket.builder()
                                .state(BucketState.builder().code("Unknown").build())
                                .build()).build());
        val result = testBucket.isStabilizedUpdate();
        verify(sdkClient, times(1)).getBuckets(any(GetBucketsRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedDelete_stabilized() {
        when(sdkClient.getBuckets(any(GetBucketsRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());
        val result = testBucket.isStabilizedDelete();
        verify(sdkClient, times(1)).getBuckets(any(GetBucketsRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedDelete_notStabilized() {
        when(sdkClient.getBuckets(any(GetBucketsRequest.class)))
                .thenReturn(GetBucketsResponse.builder()
                        .buckets(software.amazon.awssdk.services.lightsail.model.Bucket.builder()
                                .state(BucketState.builder().code("Unknown").build())
                                .build()).build());
        val result = testBucket.isStabilizedDelete();
        verify(sdkClient, times(1)).getBuckets(any(GetBucketsRequest.class));
        assertThat(result).isFalse();
    }
}
