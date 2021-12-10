package software.amazon.lightsail.alarm.helpers.resource;

import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.proxy.*;
import software.amazon.lightsail.alarm.AbstractTestBase;
import software.amazon.lightsail.alarm.ResourceModel;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.alarm.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class AlarmTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    private Logger logger;
    private Alarm testAlarm;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        logger = mock(Logger.class);

        final ResourceModel model = ResourceModel.builder().build();
        ResourceHandlerRequest<ResourceModel> resourceModelRequest =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(model)
                        .build();
        testAlarm = new Alarm(model, logger, proxyClient, resourceModelRequest);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testCreate() {
        when(sdkClient.putAlarm(any(PutAlarmRequest.class)))
                .thenReturn(PutAlarmResponse.builder().build());
        val result = testAlarm.create(PutAlarmRequest.builder().build());
        verify(sdkClient, times(1)).putAlarm(any(PutAlarmRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testUpdate() {
        when(sdkClient.putAlarm(any(PutAlarmRequest.class)))
                .thenReturn(PutAlarmResponse.builder().build());
        val result = testAlarm.update(PutAlarmRequest.builder().build());
        verify(sdkClient, times(1)).putAlarm(any(PutAlarmRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testRead() {
        when(sdkClient.getAlarms(any(GetAlarmsRequest.class)))
                .thenReturn(GetAlarmsResponse.builder().build());
        val result = testAlarm.read(GetAlarmsRequest.builder().build());
        verify(sdkClient, times(1)).getAlarms(any(GetAlarmsRequest.class));
        assertThat(result).isNotNull();
    }


    @Test
    public void testDelete() {
        when(sdkClient.deleteAlarm(any(DeleteAlarmRequest.class)))
                .thenReturn(DeleteAlarmResponse.builder().build());
        val result = testAlarm.delete(DeleteAlarmRequest.builder().build());
        verify(sdkClient, times(1)).deleteAlarm(any(DeleteAlarmRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testIsStabilizedCreate_stabilized() {
        when(sdkClient.getAlarms(any(GetAlarmsRequest.class)))
                .thenReturn(GetAlarmsResponse.builder().build());
        val result = testAlarm.isStabilizedCreate();
        verify(sdkClient, times(1)).getAlarms(any(GetAlarmsRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedCreate_notStabilized() {
        when(sdkClient.getAlarms(any(GetAlarmsRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());
        val result = testAlarm.isStabilizedCreate();
        verify(sdkClient, times(1)).getAlarms(any(GetAlarmsRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedDelete_stabilized() {
        when(sdkClient.getAlarms(any(GetAlarmsRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());
        val result = testAlarm.isStabilizedDelete();
        verify(sdkClient, times(1)).getAlarms(any(GetAlarmsRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedDelete_notStabilized() {
        when(sdkClient.getAlarms(any(GetAlarmsRequest.class)))
                .thenReturn(GetAlarmsResponse.builder().build());
        val result = testAlarm.isStabilizedDelete();
        verify(sdkClient, times(1)).getAlarms(any(GetAlarmsRequest.class));
        assertThat(result).isFalse();
    }

}
