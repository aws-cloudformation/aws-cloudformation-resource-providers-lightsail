package software.amazon.lightsail.loadbalancer;

import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.model.*;

import java.util.Collection;
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
    return CreateLoadBalancerRequest.builder().loadBalancerName(model.getLoadBalancerName()).instancePort(model.getInstancePort())
            .ipAddressType(model.getIpAddressType()).tags(translateTagsToSdk(model.getTags())).build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  public static AwsRequest translateToReadRequest(final ResourceModel model) {
    return GetLoadBalancerRequest.builder().loadBalancerName(model.getLoadBalancerName()).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  public static ResourceModel translateFromReadResponse(final AwsResponse awsResponse) {
    val getLoadBalancerResponse = (GetLoadBalancerResponse) awsResponse;
    if (getLoadBalancerResponse == null) {
      return ResourceModel.builder().build();
    }
    val loadBalancer = getLoadBalancerResponse.loadBalancer();
    return translateSDKLoadBalancerToResourceModel(loadBalancer);
  }

  private static ResourceModel translateSDKLoadBalancerToResourceModel(final LoadBalancer loadBalancer) {
    return ResourceModel.builder().loadBalancerName(loadBalancer.name()).tags(translateSDKtoTag(loadBalancer.tags()))
            .instancePort(loadBalancer.instancePort()).healthCheckPath(loadBalancer.healthCheckPath())
            .ipAddressType(loadBalancer.ipAddressTypeAsString()).loadBalancerArn(loadBalancer.arn())
            .sessionStickinessEnabled(Boolean.valueOf(loadBalancer.configurationOptions().get(LoadBalancerAttributeName.SESSION_STICKINESS_ENABLED)))
            .sessionStickinessLBCookieDurationSeconds(loadBalancer.configurationOptions().get(LoadBalancerAttributeName.SESSION_STICKINESS_LB_COOKIE_DURATION_SECONDS))
            .attachedInstances(loadBalancer.instanceHealthSummary().stream().map(instanceHealthSummary -> instanceHealthSummary.instanceName())
              .collect(Collectors.toSet())).build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  public static AwsRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteLoadBalancerRequest.builder().loadBalancerName(model.getLoadBalancerName()).build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static AwsRequest translateToListRequest(final String nextToken) {
    return GetLoadBalancersRequest.builder().pageToken(nextToken).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    val getLoadBalancersResponse = (GetLoadBalancersResponse) awsResponse;
    return getLoadBalancersResponse.loadBalancers().stream().map(Translator::translateSDKLoadBalancerToResourceModel)
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
