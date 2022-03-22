package software.amazon.lightsail.distribution;

import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.model.*;
import software.amazon.awssdk.services.lightsail.model.CacheBehavior;
import software.amazon.awssdk.services.lightsail.model.CacheBehaviorPerPath;
import software.amazon.awssdk.services.lightsail.model.CacheSettings;
import software.amazon.awssdk.services.lightsail.model.CookieObject;
import software.amazon.awssdk.services.lightsail.model.HeaderObject;
import software.amazon.awssdk.services.lightsail.model.QueryStringObject;
import software.amazon.awssdk.services.lightsail.model.InputOrigin;

import java.util.*;
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
    return CreateDistributionRequest.builder().distributionName(model.getDistributionName()).bundleId(model.getBundleId())
            .ipAddressType(model.getIpAddressType()).cacheBehaviorSettings(translateCacheBehaviorSettingsToSdk(model.getCacheBehaviorSettings()))
            .defaultCacheBehavior(CacheBehavior.builder().behavior(model.getDefaultCacheBehavior().getBehavior()).build())
            .cacheBehaviors(translateCacheBehaviorsToSdk(model.getCacheBehaviors()))
            .origin(translateInputOriginToSdk(model.getOrigin())).tags(translateTagsToSdk(model.getTags())).build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  public static AwsRequest translateToReadRequest(final ResourceModel model) {
    return GetDistributionsRequest.builder().distributionName(model.getDistributionName()).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  public static ResourceModel translateFromReadResponse(final AwsResponse awsResponse) {
    val getDistributionsResponse = (GetDistributionsResponse) awsResponse;
    if (getDistributionsResponse == null) {
      return ResourceModel.builder().build();
    }
    val distribution = getDistributionsResponse.distributions().get(0);
    return translateSDKDistributionToResourceModel(distribution);
  }

  private static ResourceModel translateSDKDistributionToResourceModel(final LightsailDistribution distribution) {
    return ResourceModel.builder().distributionName(distribution.name()).tags(translateSDKtoTag(distribution.tags()))
            .distributionArn(distribution.arn()).bundleId(distribution.bundleId()).ipAddressType(distribution.ipAddressTypeAsString())
            .isEnabled(distribution.isEnabled()).ableToUpdateBundle(distribution.ableToUpdateBundle()).status(distribution.status())
            .cacheBehaviors(distribution.cacheBehaviors().stream().
                    map(behavior -> software.amazon.lightsail.distribution.CacheBehaviorPerPath.builder()
                            .behavior(behavior.behaviorAsString()).path(behavior.path()).build()).collect(Collectors.toSet()))
            .defaultCacheBehavior(software.amazon.lightsail.distribution.CacheBehavior.builder()
                    .behavior(distribution.defaultCacheBehavior().behaviorAsString()).build())
            .cacheBehaviorSettings(translateSdkToCacheBehaviorSettings(distribution.cacheBehaviorSettings()))
            .origin(translateSdkToInputOriginObject(distribution.origin())).certificateName(distribution.certificateName())
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  public static AwsRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteDistributionRequest.builder().distributionName(model.getDistributionName()).build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  public static AwsRequest translateToUpdateRequest(final ResourceModel model) {
    return UpdateDistributionRequest.builder().distributionName(model.getDistributionName())
            .origin(translateInputOriginToSdk(model.getOrigin())).isEnabled(model.getIsEnabled())
            .defaultCacheBehavior(CacheBehavior.builder().behavior(model.getDefaultCacheBehavior().getBehavior()).build())
            .cacheBehaviorSettings(translateCacheBehaviorSettingsToSdk(model.getCacheBehaviorSettings()))
            .cacheBehaviors(translateCacheBehaviorsToSdk(model.getCacheBehaviors()))
            .build();
  }

  public static AwsRequest translateToUpdateBundleRequest(final ResourceModel model) {
    return UpdateDistributionBundleRequest.builder().distributionName(model.getDistributionName())
            .bundleId(model.getBundleId()).build();
  }

  public static AwsRequest translateToDetachCertificateRequest(final ResourceModel model) {
    return DetachCertificateFromDistributionRequest.builder().distributionName(model.getDistributionName()).build();
  }

  public static AwsRequest translateToAttachCertificateRequest(final ResourceModel model) {
    return AttachCertificateToDistributionRequest.builder().distributionName(model.getDistributionName())
            .certificateName(model.getCertificateName()).build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  public static AwsRequest translateToListRequest(final String nextToken) {
    return GetDistributionsRequest.builder().pageToken(nextToken).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    val getDistributionsResponse = (GetDistributionsResponse) awsResponse;
    return getDistributionsResponse.distributions().stream().map(Translator::translateSDKDistributionToResourceModel)
            .collect(Collectors.toList());
  }

  private static CacheSettings translateCacheBehaviorSettingsToSdk(software.amazon.lightsail.distribution.CacheSettings cacheSettings) {
    return cacheSettings == null ? null : CacheSettings.builder().allowedHTTPMethods(cacheSettings.getAllowedHTTPMethods())
            .cachedHTTPMethods(cacheSettings.getCachedHTTPMethods()).defaultTTL(cacheSettings.getDefaultTTL())
            .maximumTTL(cacheSettings.getMaximumTTL()).minimumTTL(cacheSettings.getMinimumTTL())
            .forwardedCookies(translateCookieObjectToSdk(cacheSettings.getForwardedCookies()))
            .forwardedHeaders(translateHeaderObjectToSdk(cacheSettings.getForwardedHeaders()))
            .forwardedQueryStrings(translateQueryStringObjectToSdk(cacheSettings.getForwardedQueryStrings()))
            .build();
  }

  private static List<CacheBehaviorPerPath> translateCacheBehaviorsToSdk(Collection<software.amazon.lightsail.distribution.CacheBehaviorPerPath> cacheBehaviors) {
    List<CacheBehaviorPerPath> cacheBehaviorsPerPath = new ArrayList<>();
    if (cacheBehaviors == null) {
      return cacheBehaviorsPerPath;
    }
    for (software.amazon.lightsail.distribution.CacheBehaviorPerPath behavior: cacheBehaviors.stream().collect(Collectors.toSet())) {
      cacheBehaviorsPerPath.add(CacheBehaviorPerPath.builder().behavior(behavior.getBehavior()).path(behavior.getPath()).build());
    }
    return cacheBehaviorsPerPath;
  }

  private static CookieObject translateCookieObjectToSdk(software.amazon.lightsail.distribution.CookieObject cookieObject) {
    return cookieObject == null ? null : CookieObject.builder().cookiesAllowList(cookieObject.getCookiesAllowList())
            .option(cookieObject.getOption()).build();
  }

  private static HeaderObject translateHeaderObjectToSdk(software.amazon.lightsail.distribution.HeaderObject headerObject) {
    return headerObject == null ? null : HeaderObject.builder().headersAllowListWithStrings(headerObject.getHeadersAllowList())
            .option(headerObject.getOption()).build();
  }

  private static QueryStringObject translateQueryStringObjectToSdk(software.amazon.lightsail.distribution.QueryStringObject queryStringObject) {
    return queryStringObject == null ? null : QueryStringObject.builder().queryStringsAllowList(queryStringObject.getQueryStringsAllowList())
            .option(queryStringObject.getOption()).build();
  }

  private static InputOrigin translateInputOriginToSdk(software.amazon.lightsail.distribution.InputOrigin inputOrigin) {
    return inputOrigin == null ? null : InputOrigin.builder().name(inputOrigin.getName()).protocolPolicy(inputOrigin.getProtocolPolicy())
            .regionName(inputOrigin.getRegionName()).build();
  }

  private static software.amazon.lightsail.distribution.CacheSettings translateSdkToCacheBehaviorSettings(CacheSettings cacheSettings) {
    return cacheSettings == null ? null : software.amazon.lightsail.distribution.CacheSettings.builder()
            .allowedHTTPMethods(cacheSettings.allowedHTTPMethods())
            .cachedHTTPMethods(cacheSettings.cachedHTTPMethods()).defaultTTL(cacheSettings.defaultTTL())
            .maximumTTL(cacheSettings.maximumTTL()).minimumTTL(cacheSettings.minimumTTL())
            .forwardedCookies(translateSdkToCookieObject(cacheSettings.forwardedCookies()))
            .forwardedHeaders(translateSdkToHeaderObject(cacheSettings.forwardedHeaders()))
            .forwardedQueryStrings(translateSdkToQueryStringObject(cacheSettings.forwardedQueryStrings()))
            .build();
  }

  private static software.amazon.lightsail.distribution.CookieObject translateSdkToCookieObject(CookieObject cookieObject) {
    return cookieObject == null ? null : software.amazon.lightsail.distribution.CookieObject.builder()
            .cookiesAllowList(cookieObject.cookiesAllowList().stream().collect(Collectors.toSet()))
            .option(cookieObject.optionAsString()).build();
  }

  private static software.amazon.lightsail.distribution.HeaderObject translateSdkToHeaderObject(HeaderObject headerObject) {
    return headerObject == null ? null : software.amazon.lightsail.distribution.HeaderObject.builder().
            headersAllowList(headerObject.headersAllowListAsStrings().stream().collect(Collectors.toSet()))
            .option(headerObject.optionAsString()).build();
  }

  private static software.amazon.lightsail.distribution.QueryStringObject translateSdkToQueryStringObject(QueryStringObject queryStringObject) {
    return queryStringObject == null ? null : software.amazon.lightsail.distribution.QueryStringObject.builder()
            .queryStringsAllowList(queryStringObject.queryStringsAllowList().stream().collect(Collectors.toSet()))
            .option(queryStringObject.option()).build();
  }

  private static software.amazon.lightsail.distribution.InputOrigin translateSdkToInputOriginObject(Origin origin) {
    return origin == null ? null : software.amazon.lightsail.distribution.InputOrigin.builder()
            .name(origin.name()).protocolPolicy(origin.protocolPolicyAsString())
            .regionName(origin.regionNameAsString()).build();
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
