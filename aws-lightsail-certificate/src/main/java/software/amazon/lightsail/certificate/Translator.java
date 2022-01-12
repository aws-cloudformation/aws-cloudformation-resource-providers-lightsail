package software.amazon.lightsail.certificate;

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
    return CreateCertificateRequest.builder().certificateName(model.getCertificateName()).domainName(model.getDomainName())
            .subjectAlternativeNames(model.getSubjectAlternativeNames()).tags(translateTagsToSdk(model.getTags())).build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  public static AwsRequest translateToReadRequest(final ResourceModel model) {
    return GetCertificatesRequest.builder().certificateName(model.getCertificateName()).includeCertificateDetails(true).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  public static ResourceModel translateFromReadResponse(final AwsResponse awsResponse) {
    val getCertificatesResponse = (GetCertificatesResponse) awsResponse;
    if (getCertificatesResponse == null) {
      return ResourceModel.builder().build();
    }
    val certificate = getCertificatesResponse.certificates().get(0);
    return translateSDKCertificateToResourceModel(certificate);
  }

  private static ResourceModel translateSDKCertificateToResourceModel(final CertificateSummary certificate) {
    return ResourceModel.builder().certificateName(certificate.certificateName()).domainName(certificate.domainName())
            .certificateArn(certificate.certificateArn()).status(certificate.certificateDetail().statusAsString())
            .subjectAlternativeNames(certificate.certificateDetail().subjectAlternativeNames().stream()
                    .filter(name -> !name.equals(certificate.domainName())).collect(Collectors.toSet()))
            .tags(translateSDKtoTag(certificate.tags())).build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  public static AwsRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteCertificateRequest.builder().certificateName(model.getCertificateName()).build();
  }

  /**
   * Request to list resources
   * @return awsRequest the aws service request to list resources within aws account
   */
  static AwsRequest translateToListRequest() {
    return GetCertificatesRequest.builder().includeCertificateDetails(true).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    val getCertificatesResponse = (GetCertificatesResponse) awsResponse;
    return getCertificatesResponse.certificates().stream().map(Translator::translateSDKCertificateToResourceModel)
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
