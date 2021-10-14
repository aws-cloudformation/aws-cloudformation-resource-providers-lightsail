package software.amazon.lightsail.database.helpers.resource;

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
import software.amazon.lightsail.database.AbstractTestBase;
import software.amazon.lightsail.database.ResourceModel;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static software.amazon.lightsail.database.AbstractTestBase.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class DatabaseTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<LightsailClient> proxyClient;

    @Mock
    LightsailClient sdkClient;

    @Mock
    private Database database;

    private Logger logger;
    private Database testDatabase;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(LightsailClient.class);
        proxyClient = AbstractTestBase.MOCK_PROXY(proxy, sdkClient);
        database = mock(Database.class);
        logger = mock(Logger.class);

        final ResourceModel model = ResourceModel.builder()
                .publiclyAccessible(true).backupRetention(true).build();
        testDatabase = new Database(model, logger, proxyClient, null);
    }

    @AfterEach
    public void tear_down() {

    }

    @Test
    public void testCreate() {
        when(sdkClient.createRelationalDatabase(any(CreateRelationalDatabaseRequest.class)))
                .thenReturn(CreateRelationalDatabaseResponse.builder().build());
        val result = testDatabase.create(CreateRelationalDatabaseRequest.builder().build());
        verify(sdkClient, times(1)).createRelationalDatabase(any(CreateRelationalDatabaseRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testRead() {
        when(sdkClient.getRelationalDatabase(any(GetRelationalDatabaseRequest.class)))
                .thenReturn(GetRelationalDatabaseResponse.builder().build());
        val result = testDatabase.read(GetRelationalDatabaseRequest.builder().build());
        verify(sdkClient, times(1)).getRelationalDatabase(any(GetRelationalDatabaseRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testDelete() {
        when(sdkClient.deleteRelationalDatabase(any(DeleteRelationalDatabaseRequest.class)))
                .thenReturn(DeleteRelationalDatabaseResponse.builder().build());
        val result = testDatabase.delete(DeleteRelationalDatabaseRequest.builder().build());
        verify(sdkClient, times(1)).deleteRelationalDatabase(any(DeleteRelationalDatabaseRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testUpdate_noUpdateRequired() {
        when(sdkClient.getRelationalDatabase(any(GetRelationalDatabaseRequest.class)))
                .thenReturn(GetRelationalDatabaseResponse.builder()
                        .relationalDatabase(RelationalDatabase.builder()
                            .publiclyAccessible(true).backupRetentionEnabled(true)
                        .build()).build());
        val result = testDatabase.update(UpdateRelationalDatabaseRequest.builder().build());
        verify(sdkClient, never()).updateRelationalDatabase(any(UpdateRelationalDatabaseRequest.class));
        assertThat(result).isNull();
    }

    @Test
    public void testUpdate_updateRequired() {
        when(sdkClient.getRelationalDatabase(any(GetRelationalDatabaseRequest.class)))
                .thenReturn(GetRelationalDatabaseResponse.builder()
                        .relationalDatabase(RelationalDatabase.builder()
                                .publiclyAccessible(false).backupRetentionEnabled(false)
                                .build()).build());
        when(sdkClient.updateRelationalDatabase(any(UpdateRelationalDatabaseRequest.class)))
                .thenReturn(UpdateRelationalDatabaseResponse.builder().build());
        val result = testDatabase.update(UpdateRelationalDatabaseRequest.builder().build());
        verify(sdkClient, times(1)).updateRelationalDatabase(any(UpdateRelationalDatabaseRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void testIsStabilizedCreate_stabilized() {
        when(sdkClient.getRelationalDatabase(any(GetRelationalDatabaseRequest.class)))
                .thenReturn(GetRelationalDatabaseResponse.builder()
                        .relationalDatabase(RelationalDatabase.builder()
                                .state("available")
                                .build()).build());
        val result = testDatabase.isStabilizedCreate();
        verify(sdkClient, times(1)).getRelationalDatabase(any(GetRelationalDatabaseRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedCreate_notStabilized() {
        when(sdkClient.getRelationalDatabase(any(GetRelationalDatabaseRequest.class)))
                .thenReturn(GetRelationalDatabaseResponse.builder()
                        .relationalDatabase(RelationalDatabase.builder()
                                .state("modifying")
                                .build()).build());
        val result = testDatabase.isStabilizedCreate();
        verify(sdkClient, times(1)).getRelationalDatabase(any(GetRelationalDatabaseRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedUpdate_stabilized() {
        when(sdkClient.getRelationalDatabase(any(GetRelationalDatabaseRequest.class)))
                .thenReturn(GetRelationalDatabaseResponse.builder()
                        .relationalDatabase(RelationalDatabase.builder()
                                .state("available")
                                .build()).build());
        val result = testDatabase.isStabilizedUpdate();
        verify(sdkClient, times(1)).getRelationalDatabase(any(GetRelationalDatabaseRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedUpdate_notStabilized() {
        when(sdkClient.getRelationalDatabase(any(GetRelationalDatabaseRequest.class)))
                .thenReturn(GetRelationalDatabaseResponse.builder()
                        .relationalDatabase(RelationalDatabase.builder()
                                .state("modifying")
                                .build()).build());
        val result = testDatabase.isStabilizedUpdate();
        verify(sdkClient, times(1)).getRelationalDatabase(any(GetRelationalDatabaseRequest.class));
        assertThat(result).isFalse();
    }

    @Test
    public void testIsStabilizedDelete_stabilized() {
        when(sdkClient.getRelationalDatabase(any(GetRelationalDatabaseRequest.class)))
                .thenThrow(NotFoundException.builder()
                        .awsErrorDetails(AwsErrorDetails
                                .builder().errorCode("NotFoundException")
                                .build()).build());
        val result = testDatabase.isStabilizedDelete();
        verify(sdkClient, times(1)).getRelationalDatabase(any(GetRelationalDatabaseRequest.class));
        assertThat(result).isTrue();
    }

    @Test
    public void testIsStabilizedDelete_notStabilized() {
        when(sdkClient.getRelationalDatabase(any(GetRelationalDatabaseRequest.class)))
                .thenReturn(GetRelationalDatabaseResponse.builder()
                        .relationalDatabase(RelationalDatabase.builder()
                                .state("modifying")
                                .build()).build());
        val result = testDatabase.isStabilizedDelete();
        verify(sdkClient, times(1)).getRelationalDatabase(any(GetRelationalDatabaseRequest.class));
        assertThat(result).isFalse();
    }
}
