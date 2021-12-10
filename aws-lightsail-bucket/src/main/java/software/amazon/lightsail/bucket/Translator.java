package software.amazon.lightsail.bucket;

import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.model.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  public static AwsRequest translateToCreateRequest(final ResourceModel model) {
    return CreateBucketRequest.builder().bucketName(model.getBucketName())
            .bundleId(model.getBundleId()).enableObjectVersioning(model.getObjectVersioning())
            .tags(translateTagsToSdk(model.getTags())).build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  public static AwsRequest translateToReadRequest(final ResourceModel model) {
    return GetBucketsRequest.builder().bucketName(model.getBucketName()).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  public static ResourceModel translateFromReadResponse(final AwsResponse awsResponse) {
    val getBucketResponse = (GetBucketsResponse) awsResponse;
    if (getBucketResponse == null) {
      return ResourceModel.builder().build();
    }
    val bucket = getBucketResponse.buckets().get(0);
    return translateSDKBucketToResourceModel(bucket);
  }

  private static ResourceModel translateSDKBucketToResourceModel(final Bucket bucket) {
    return ResourceModel.builder().bucketName(bucket.name()).tags(translateSDKtoTag(bucket.tags()))
            .bundleId(bucket.bundleId()).url(bucket.url()).ableToUpdateBundle(bucket.ableToUpdateBundle())
            .accessRules(AccessRules.builder().getObject(bucket.accessRules().getObjectAsString())
                    .allowPublicOverrides(bucket.accessRules().allowPublicOverrides()).build())
            .resourcesReceivingAccess(bucket.resourcesReceivingAccess().stream().map(resource -> resource.name()).collect(Collectors.toSet()))
            .readOnlyAccessAccounts(bucket.readonlyAccessAccounts().stream().collect(Collectors.toSet()))
            .objectVersioning(bucket.objectVersioning().equalsIgnoreCase("Enabled") ? true : false)
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  public static AwsRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteBucketRequest.builder().bucketName(model.getBucketName()).forceDelete(true).build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  public static AwsRequest translateToUpdateRequest(final ResourceModel model) {
    return UpdateBucketRequest.builder().bucketName(model.getBucketName())
            .accessRules(model.getAccessRules() == null ? software.amazon.awssdk.services.lightsail.model.AccessRules.builder()
                    .getObject("private").allowPublicOverrides(false).build() : software.amazon.awssdk.services.lightsail.model.AccessRules.builder()
                    .getObject(model.getAccessRules().getGetObject()).allowPublicOverrides(model.getAccessRules().getAllowPublicOverrides()).build())
            .versioning((model.getObjectVersioning() != null && model.getObjectVersioning()) ? "Enabled" : "Suspended")
            .readonlyAccessAccounts(model.getReadOnlyAccessAccounts() == null ? new HashSet<>() : model.getReadOnlyAccessAccounts())
            .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  public static AwsRequest translateToUpdateBundleRequest(final ResourceModel model) {
    return UpdateBucketBundleRequest.builder().bucketName(model.getBucketName())
            .bundleId(model.getBundleId()).build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static AwsRequest translateToListRequest(final String nextToken) {
    return GetBucketsRequest.builder().pageToken(nextToken).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    val getBucketsResponse = (GetBucketsResponse) awsResponse;
    return getBucketsResponse.buckets().stream().map(Translator::translateSDKBucketToResourceModel)
            .collect(Collectors.toList());
  }

  private static Set<software.amazon.awssdk.services.lightsail.model.Tag> translateTagsToSdk(Collection<Tag> tags) {
    return tags == null ? null : tags.stream().map(tag -> software.amazon.awssdk.services.lightsail.model.Tag
            .builder().key(tag.getKey()).value(tag.getValue()).build()).collect(Collectors.toSet());
  }

  private static Set<Tag> translateSDKtoTag(List<software.amazon.awssdk.services.lightsail.model.Tag> tags) {
    return tags == null ? null : tags.stream().map(tag -> Tag.builder().key(tag.key())
            .value((tag.value() != null && tag.value().length() == 0) ? null : tag.value()).build()).collect(Collectors.toSet());
  }
}
