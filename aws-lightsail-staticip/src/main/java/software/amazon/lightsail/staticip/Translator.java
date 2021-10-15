package software.amazon.lightsail.staticip;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.model.*;

import lombok.val;
import java.util.List;
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
    return AllocateStaticIpRequest.builder().staticIpName(model.getStaticIpName()).build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  public static AwsRequest translateToReadRequest(final ResourceModel model) {
    return GetStaticIpRequest.builder().staticIpName(model.getStaticIpName()).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  public static ResourceModel translateFromReadResponse(final AwsResponse awsResponse) {
    val getStaticIpResponse = (GetStaticIpResponse) awsResponse;
    if (getStaticIpResponse == null) {
      return ResourceModel.builder().build();
    }
    val staticIp = getStaticIpResponse.staticIp();
    return translateSDKStaticIpToResourceModel(staticIp);
  }

  private static ResourceModel translateSDKStaticIpToResourceModel(final StaticIp staticIp) {
    return ResourceModel.builder().staticIpName(staticIp.name()).attachedTo(staticIp.attachedTo())
            .isAttached(staticIp.isAttached()).ipAddress(staticIp.ipAddress()).staticIpArn(staticIp.arn()).build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  public static AwsRequest translateToDeleteRequest(final ResourceModel model) {
    return ReleaseStaticIpRequest.builder().staticIpName(model.getStaticIpName()).build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static AwsRequest translateToListRequest(final String nextToken) {
    return GetStaticIpsRequest.builder().pageToken(nextToken).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    val getStaticIpsResponse = (GetStaticIpsResponse) awsResponse;
    return getStaticIpsResponse.staticIps().stream().map(Translator::translateSDKStaticIpToResourceModel)
            .collect(Collectors.toList());
  }
}
