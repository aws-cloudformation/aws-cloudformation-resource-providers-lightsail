package software.amazon.lightsail.database;

import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is a centralized placeholder for - api request construction - object translation to/from aws sdk -
 * resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   *
   * @param model
   *            resource model
   *
   * @return awsRequest the aws service request to create a resource
   */
  public static AwsRequest translateToCreateRequest(final ResourceModel model) {
    CreateRelationalDatabaseRequest.Builder createRequest = CreateRelationalDatabaseRequest.builder();
    createRequest.relationalDatabaseName(model.getRelationalDatabaseName())
            .relationalDatabaseBlueprintId(model.getRelationalDatabaseBlueprintId())
            .relationalDatabaseBundleId(model.getRelationalDatabaseBundleId())
            .availabilityZone(model.getAvailabilityZone()).masterUsername(model.getMasterUsername())
            .masterDatabaseName(model.getMasterDatabaseName()).tags(translateTagsToSdk(model.getTags()))
            .masterUserPassword(model.getMasterUserPassword()).publiclyAccessible(model.getPubliclyAccessible())
            .preferredMaintenanceWindow(model.getPreferredMaintenanceWindow()).preferredBackupWindow(model.getPreferredBackupWindow());
    return createRequest.build();
  }

  /**
   * Request to update a resource
   *
   * @param model
   *            resource model
   *
   * @return awsRequest the aws service request to update a resource
   */
  public static AwsRequest translateToUpdateRequest(final ResourceModel model) {
    UpdateRelationalDatabaseRequest.Builder updateRequest = UpdateRelationalDatabaseRequest.builder();
    updateRequest.relationalDatabaseName(model.getRelationalDatabaseName()).applyImmediately(true) // applyImmediately is always set to true for updates
    .caCertificateIdentifier(model.getCaCertificateIdentifier()).masterUserPassword(model.getMasterUserPassword())
    .preferredBackupWindow(model.getPreferredBackupWindow()).preferredMaintenanceWindow(model.getPreferredMaintenanceWindow())
    .publiclyAccessible(model.getPubliclyAccessible()).rotateMasterUserPassword(model.getRotateMasterUserPassword());
    if (model.getBackupRetention() != null) {
      if (model.getBackupRetention()) {
        updateRequest.enableBackupRetention(true);
      } else {
        updateRequest.disableBackupRetention(true);
      }
    }
    return updateRequest.build();
  }

  /**
   * Request to update parameters of the resource
   *
   * @param model
   *            resource model
   *
   * @return awsRequest the aws service request to update the parameters of the resource
   */
  public static AwsRequest translateToUpdateParametersRequest(final ResourceModel model) {
    UpdateRelationalDatabaseParametersRequest.Builder updateParametersRequest = UpdateRelationalDatabaseParametersRequest.builder();
    updateParametersRequest.relationalDatabaseName(model.getRelationalDatabaseName());
    List<software.amazon.awssdk.services.lightsail.model.RelationalDatabaseParameter> relationalDatabaseParameters = new ArrayList<>();
    if (model.getRelationalDatabaseParameters() == null) {
      return updateParametersRequest.build();
    }
    for (RelationalDatabaseParameter dbParameter : model.getRelationalDatabaseParameters()) {
      software.amazon.awssdk.services.lightsail.model.RelationalDatabaseParameter.Builder parameter =
              software.amazon.awssdk.services.lightsail.model.RelationalDatabaseParameter.builder();
      parameter.allowedValues(dbParameter.getAllowedValues());
      parameter.applyMethod(dbParameter.getApplyMethod());
      parameter.applyType(dbParameter.getApplyType());
      parameter.dataType(dbParameter.getDataType());
      parameter.description(dbParameter.getDescription());
      parameter.isModifiable(dbParameter.getIsModifiable());
      parameter.parameterName(dbParameter.getParameterName());
      parameter.parameterValue(dbParameter.getParameterValue());
      relationalDatabaseParameters.add(parameter.build());
    }
    updateParametersRequest.parameters(relationalDatabaseParameters);
    return updateParametersRequest.build();
  }

  /**
   * Request to read a resource
   *
   * @param model
   *            resource model
   *
   * @return awsRequest the aws service request to describe a resource
   */
  public static AwsRequest translateToReadRequest(final ResourceModel model) {
    return GetRelationalDatabaseRequest.builder().relationalDatabaseName(model.getRelationalDatabaseName()).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   *
   * @param awsResponse
   *            the aws service describe resource response
   *
   * @return model resource model
   */
  public static ResourceModel translateFromReadResponse(final AwsResponse awsResponse) {
    val getDatabaseResponse = (GetRelationalDatabaseResponse) awsResponse;
    if (getDatabaseResponse == null) {
      return ResourceModel.builder().build();
    }
    val database = getDatabaseResponse.relationalDatabase();
    return translateSDKDatabaseToResourceModel(database);
  }

  private static ResourceModel translateSDKDatabaseToResourceModel(final RelationalDatabase database) {
    return ResourceModel.builder().availabilityZone(database.location() == null ? null : database.location().availabilityZone())
            .relationalDatabaseName(database.name()).tags(translateSDKtoTag(database.tags()))
            .backupRetention(database.backupRetentionEnabled())
            .caCertificateIdentifier(database.caCertificateIdentifier())
            .masterDatabaseName(database.masterDatabaseName())
            .masterUsername(database.masterUsername())
            .preferredBackupWindow(database.preferredBackupWindow())
            .preferredMaintenanceWindow(database.preferredMaintenanceWindow())
            .publiclyAccessible(database.publiclyAccessible())
            .relationalDatabaseBlueprintId(database.relationalDatabaseBlueprintId())
            .relationalDatabaseBundleId(database.relationalDatabaseBundleId())
            .databaseArn(database.arn())
            .build();
  }

  /**
   * Request to delete a resource
   *
   * @param model
   *            resource model
   *
   * @return awsRequest the aws service request to delete a resource
   */
  public static AwsRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteRelationalDatabaseRequest.builder().relationalDatabaseName(model.getRelationalDatabaseName())
            .skipFinalSnapshot(true).build();
  }

  /**
   * Request to list resources
   *
   * @param nextToken
   *            token passed to the aws service list resources request
   *
   * @return awsRequest the aws service request to list resources within aws account
   */
  static AwsRequest translateToListRequest(final String nextToken) {
    return GetRelationalDatabasesRequest.builder().pageToken(nextToken).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   *
   * @param awsResponse
   *            the aws service describe resource response
   *
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    val getDatabasesResponse = (GetRelationalDatabasesResponse) awsResponse;
    return getDatabasesResponse.relationalDatabases().stream().map(Translator::translateSDKDatabaseToResourceModel)
            .collect(Collectors.toList());
  }

  private static Set<software.amazon.awssdk.services.lightsail.model.Tag> translateTagsToSdk(Collection<Tag> tags) {
    return tags == null ? null : tags.stream().map(tag -> software.amazon.awssdk.services.lightsail.model.Tag
            .builder().key(tag.getKey()).value(tag.getValue()).build()).collect(Collectors.toSet());
  }

  private static Set<Tag> translateSDKtoTag(List<software.amazon.awssdk.services.lightsail.model.Tag> tags) {
    return tags == null ? null : tags.stream().map(tag -> Tag.builder().key(tag.key()).value(tag.value()).build())
            .collect(Collectors.toSet());
  }

}
