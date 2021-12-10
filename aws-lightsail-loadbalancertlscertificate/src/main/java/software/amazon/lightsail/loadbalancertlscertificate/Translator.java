package software.amazon.lightsail.loadbalancertlscertificate;

import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.model.*;

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
    return CreateLoadBalancerTlsCertificateRequest.builder().certificateName(model.getCertificateName())
            .loadBalancerName(model.getLoadBalancerName()).certificateDomainName(model.getCertificateDomainName())
            .certificateAlternativeNames(model.getCertificateAlternativeNames()).build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  public static AwsRequest translateToReadRequest(final ResourceModel model) {
    return GetLoadBalancerTlsCertificatesRequest.builder().loadBalancerName(model.getLoadBalancerName()).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  public static ResourceModel translateFromReadResponse(final AwsResponse awsResponse, final ResourceModel model) {
    val getLoadBalancerTlsCertificatesResponse = (GetLoadBalancerTlsCertificatesResponse) awsResponse;
    if (getLoadBalancerTlsCertificatesResponse == null) {
      return ResourceModel.builder().build();
    }
    val certificate = getLoadBalancerTlsCertificatesResponse.tlsCertificates().stream()
            .filter(loadBalancerTlsCertificate -> loadBalancerTlsCertificate.name().equals(model.getCertificateName()))
            .collect(Collectors.toList()).get(0);
    return translateSDKLoadBalancerTlsCertificateToResourceModel(certificate);
  }

  private static ResourceModel translateSDKLoadBalancerTlsCertificateToResourceModel(final LoadBalancerTlsCertificate certificate) {
    return ResourceModel.builder().loadBalancerName(certificate.loadBalancerName())
            .certificateName(certificate.name()).certificateDomainName(certificate.domainName()).loadBalancerTlsCertificateArn(certificate.arn())
            .certificateAlternativeNames(certificate.subjectAlternativeNames().stream()
                    .filter(name -> !name.equals(certificate.domainName())).collect(Collectors.toSet()))
            .isAttached(certificate.isAttached()).status(certificate.status().name()).build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  public static AwsRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteLoadBalancerTlsCertificateRequest.builder().loadBalancerName(model.getLoadBalancerName())
            .certificateName(model.getCertificateName()).force(true).build();
  }

  /**
   * Request to list resources
   * @param model resource model
   * @return awsRequest the aws service request to list resources within aws account
   */
  static AwsRequest translateToListRequest(final ResourceModel model) {
    return GetLoadBalancerTlsCertificatesRequest.builder().loadBalancerName(model.getLoadBalancerName()).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    val getLoadBalancerTlsCertificatesResponse = (GetLoadBalancerTlsCertificatesResponse) awsResponse;
    return getLoadBalancerTlsCertificatesResponse.tlsCertificates().stream().map(Translator::translateSDKLoadBalancerTlsCertificateToResourceModel)
            .collect(Collectors.toList());
  }

}
