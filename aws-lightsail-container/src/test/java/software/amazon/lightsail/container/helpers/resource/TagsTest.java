package software.amazon.lightsail.container.helpers.resource;

import com.google.common.collect.ImmutableSet;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.container.AbstractTestBase;
import software.amazon.lightsail.container.ResourceModel;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.container.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class TagsTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    @Mock
    private Container container;

    private Logger logger;
    private Tags testTags;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        container = mock(Container.class);
        logger = mock(Logger.class);

        final ResourceModel model = ResourceModel.builder()
                .tags(ImmutableSet.of(
                software.amazon.lightsail.container.Tag.builder().key("key1").value("value1").build(),
                        software.amazon.lightsail.container.Tag.builder().key("key2").value("value2").build(),
                        software.amazon.lightsail.container.Tag.builder().key("key3").value("value3").build(),
                        software.amazon.lightsail.container.Tag.builder().key("key1").value("value2").build(),
                        software.amazon.lightsail.container.Tag.builder().key("key4").value("value3").build(),
                        software.amazon.lightsail.container.Tag.builder().key("key5").build())
                ).build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        testTags = new Tags(model, logger, proxyClient, resourceModelRequest);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void update() {
        val basicResponse = GetContainerServicesResponse.builder()
                .containerServices(ContainerService.builder()
                        .tags(ImmutableSet.of(
                                Tag.builder()
                                        .key("key1").value("value1").build(),
                                Tag.builder()
                                        .key("key2").value("value5").build(), // different value than what is there.
                                Tag.builder()
                                        .key("key6").value("value6").build(), // new Key new Value
                                Tag.builder()
                                        .key("key6").value("value2").build(), // new Key old Value.
                                Tag.builder()
                                        .key("key4").build())) // only old key, no value
                        .build()).build();

        when(sdkClient.getContainerServices(any(GetContainerServicesRequest.class)))
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse);

        testTags.update(mock(AwsRequest.class));

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
    public void updateOnlyRemove() {
        val basicResponse = GetContainerServicesResponse.builder()
                .containerServices(ContainerService.builder()
                        .tags(ImmutableSet.of(
                                Tag.builder()
                                        .key("key1").value("value1").build(),
                                Tag.builder()
                                        .key("key2").value("value2").build(),
                                Tag.builder()
                                        .key("key3").value("value3").build(),
                                Tag.builder()
                                        .key("key1").value("value2").build(),
                                Tag.builder()
                                        .key("key4").value("value3").build(),
                                Tag.builder()
                                        .key("key5").build(),
                                Tag.builder()
                                        .key("key10").value("value10").build(),
                                Tag.builder()
                                        .key("key11").value("value11").build(),
                                Tag.builder()
                                        .key("key12").value("value12").build()))
                        .build()).build();

        when(sdkClient.getContainerServices(any(GetContainerServicesRequest.class)))
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse);

        testTags.update(mock(AwsRequest.class));

        ArgumentCaptor<TagResourceRequest> captor1 = ArgumentCaptor.forClass(TagResourceRequest.class);
        ArgumentCaptor<UntagResourceRequest> captor2 = ArgumentCaptor.forClass(UntagResourceRequest.class);
        verify(sdkClient, times(0)).tagResource(captor1.capture());
        verify(sdkClient, times(1)).untagResource(captor2.capture());
        assertNotNull(captor2.getValue());
        assertNotNull(captor2.getValue().tagKeys());
        assertEquals(3, captor2.getValue().tagKeys().size());
    }

    @Test
    public void updateOnlyAdd() {
        val basicResponse = GetContainerServicesResponse.builder()
                .containerServices(ContainerService.builder()
                        .tags(ImmutableSet.of(
                                Tag.builder()
                                        .key("key1").value("value1").build(),
                                Tag.builder()
                                        .key("key2").value("value2").build(),
                                Tag.builder()
                                        .key("key3").value("value3").build()))
                        .build()).build();

        when(sdkClient.getContainerServices(any(GetContainerServicesRequest.class)))
                .thenReturn(basicResponse)
                .thenReturn(basicResponse)
                .thenReturn(basicResponse);

        testTags.update(mock(AwsRequest.class));

        ArgumentCaptor<TagResourceRequest> captor1 = ArgumentCaptor.forClass(TagResourceRequest.class);
        ArgumentCaptor<UntagResourceRequest> captor2 = ArgumentCaptor.forClass(UntagResourceRequest.class);
        verify(sdkClient, times(1)).tagResource(captor1.capture());
        verify(sdkClient, times(0)).untagResource(captor2.capture());
        assertNotNull(captor1.getValue());
        assertNotNull(captor1.getValue().tags());
        assertEquals(3, captor1.getValue().tags().size());
    }

    @Test
    public void testUnSupportedMethods() {
        try {
            testTags.create(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testTags.delete(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testTags.read(mock(AwsRequest.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testTags.isStabilizedUpdate();
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testTags.isStabilizedDelete();
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testTags.isSafeExceptionCreateOrUpdate(NotFoundException.builder().build());
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }

        try {
            testTags.isSafeExceptionDelete(NotFoundException.builder().build());
            fail();
        } catch (UnsupportedOperationException e) {
            // pass the exception
        }
    }
}
