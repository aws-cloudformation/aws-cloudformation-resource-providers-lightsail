package software.amazon.lightsail.database.helpers.resource;

import lombok.RequiredArgsConstructor;
import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.lightsail.database.ResourceModel;

/**
 * Helper class to handle Database operations.
 */
@RequiredArgsConstructor
public class Database implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    public AwsResponse update(AwsRequest request) {
        AwsResponse awsResponse = null;
        if (!isUpdateRequired()) {
            logger.log(String.format("Update not required for Relational Database: %s", resourceModel.getRelationalDatabaseName()));
            return awsResponse;
        }
        logger.log(String.format("Updating Relational Database: %s", resourceModel.getRelationalDatabaseName()));
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((UpdateRelationalDatabaseRequest) request),
                proxyClient.client()::updateRelationalDatabase);
        logger.log(String.format("Successfully updated Relational Database: %s", resourceModel.getRelationalDatabaseName()));
        return awsResponse;
    }

    public AwsResponse updateParameters(AwsRequest request) {
        AwsResponse awsResponse = null;
        if (resourceModel.getRelationalDatabaseParameters() == null) {
            logger.log(String.format("Update parameters not required for Relational Database: %s", resourceModel.getRelationalDatabaseName()));
            return awsResponse;
        }
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((UpdateRelationalDatabaseParametersRequest) request),
                proxyClient.client()::updateRelationalDatabaseParameters);
        logger.log(String.format("Successfully updated Relational Database Parameters for: %s", resourceModel.getRelationalDatabaseName()));
        return awsResponse;
    }

    @Override
    public AwsResponse create(AwsRequest request) {
        logger.log(String.format("Creating Relational Database: %s", resourceModel.getRelationalDatabaseName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((CreateRelationalDatabaseRequest) request),
                proxyClient.client()::createRelationalDatabase);
        logger.log(String.format("Successfully created Relational Database: %s", resourceModel.getRelationalDatabaseName()));
        return awsResponse;
    }

    @Override
    public AwsResponse delete(AwsRequest request) {
        logger.log(String.format("Deleting Relational Database: %s", resourceModel.getRelationalDatabaseName()));
        AwsResponse awsResponse = null;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((DeleteRelationalDatabaseRequest) request),
                proxyClient.client()::deleteRelationalDatabase);
        logger.log(String.format("Successfully deleted Relational Database: %s", resourceModel.getRelationalDatabaseName()));
        return awsResponse;
    }

    /**
     * Read Database.
     *
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse read(AwsRequest request) {
        val databaseName = ((GetRelationalDatabaseRequest) request).relationalDatabaseName();
        logger.log(String.format("Reading Relational Database: %s", databaseName));
        return proxyClient.injectCredentialsAndInvokeV2(GetRelationalDatabaseRequest.builder().relationalDatabaseName(databaseName).build(),
                proxyClient.client()::getRelationalDatabase);
    }

    @Override
    public boolean isStabilizedUpdate() {
        val awsResponse = ((GetRelationalDatabaseResponse) this
                .read(GetRelationalDatabaseRequest.builder().relationalDatabaseName(resourceModel.getRelationalDatabaseName()).build()));
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Relational Database: %s has stabilized. Current state: %s",
                resourceModel.getRelationalDatabaseName(), currentState));
        return ("available".equalsIgnoreCase(currentState));
    }

    public boolean isStabilizedCreate() {
        val awsResponse = ((GetRelationalDatabaseResponse) this
                .read(GetRelationalDatabaseRequest.builder().relationalDatabaseName(resourceModel.getRelationalDatabaseName()).build()));
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Relational Database: %s has stabilized. Current state: %s",
                resourceModel.getRelationalDatabaseName(), currentState));
        return ("available".equalsIgnoreCase(currentState));
    }

    @Override
    public boolean isStabilizedDelete() {
        final boolean stabilized = false;
        logger.log(String.format("Checking if Relational Database: %s deletion has stabilized.",
                resourceModel.getRelationalDatabaseName(), stabilized));
        try {
            this.read(GetRelationalDatabaseRequest.builder().relationalDatabaseName(resourceModel.getMasterDatabaseName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            logger.log(String.format("Relational Database: %s deletion has stabilized", resourceModel.getRelationalDatabaseName()));
            return true;
        }
        return stabilized;
    }

    /**
     * Get Current state of the Database.
     *
     * @return
     *
     * @param awsResponse
     */
    private String getCurrentState(GetRelationalDatabaseResponse awsResponse) {
        val database = awsResponse.relationalDatabase();
        return database.state() == null ? "Pending" : database.state();
    }

    @Override
    public boolean isSafeExceptionCreateOrUpdate(Exception e) {
        return false;
    }

    @Override
    public boolean isSafeExceptionDelete(Exception e) {
        if (e instanceof CfnNotFoundException || e instanceof NotFoundException) {
            return true; // Its stabilized if the resource is gone..
        }
        return false;
    }

    /**
     * Checking to see if update is required for the relational database.
     *
     * @return
     */
    public boolean isUpdateRequired() {
        val database = ((GetRelationalDatabaseResponse) this.read(GetRelationalDatabaseRequest.builder()
                .relationalDatabaseName(resourceModel.getRelationalDatabaseName()).build())).relationalDatabase();
        if ((resourceModel.getPubliclyAccessible() != null && resourceModel.getPubliclyAccessible() != database.publiclyAccessible()) ||
                (resourceModel.getBackupRetention() != null && resourceModel.getBackupRetention() != database.backupRetentionEnabled()) ||
                (resourceModel.getCaCertificateIdentifier() != null && !(resourceModel.getCaCertificateIdentifier().equalsIgnoreCase(database.caCertificateIdentifier()))) ||
                (resourceModel.getPreferredBackupWindow() != null && !(resourceModel.getPreferredBackupWindow().equalsIgnoreCase(database.preferredBackupWindow()))) ||
                (resourceModel.getPreferredMaintenanceWindow() != null && !(resourceModel.getPreferredMaintenanceWindow().equalsIgnoreCase(database.preferredMaintenanceWindow()))) ||
                (resourceModel.getRotateMasterUserPassword() != null && resourceModel.getRotateMasterUserPassword()) ||
                (resourceModel.getRotateMasterUserPassword() != null)) {
            return true;
        }
        return false;
    }

    /**
     * Get the first sorted availability zone in the current region.
     *
     * @return String
     */
    public String getFirstAvailabilityZone() {
        return proxyClient
                .injectCredentialsAndInvokeV2(GetRegionsRequest.builder().includeAvailabilityZones(true).build(),
                        proxyClient.client()::getRegions)
                .regions().stream()
                .filter(region -> resourceModelRequest.getRegion().equalsIgnoreCase(region.name().toString()))
                .filter(region -> !region.availabilityZones().isEmpty()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Something wrong with fetching current region"))
                .availabilityZones().stream().map(AvailabilityZone::zoneName).sorted().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Something wrong with " + "fetching availability zone for current region"));
    }
}
