package software.amazon.lightsail.container;

import lombok.val;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.lightsail.model.*;

import javax.annotation.Resource;
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
    return CreateContainerServiceRequest.builder().serviceName(model.getServiceName()).power(model.getPower()).scale(model.getScale())
            .publicDomainNames(translatePublicDomainNamesToSDK(model.getPublicDomainNames(), model)).tags(translateTagsToSdk(model.getTags())).build();
  }

  public static AwsRequest translateToCreateContainerServiceDeploymentRequest(final ResourceModel model) {
    return CreateContainerServiceDeploymentRequest.builder().serviceName(model.getServiceName())
            .containers(translateContainersToSDK(model.getContainerServiceDeployment() == null ? null : model.getContainerServiceDeployment().getContainers()))
            .publicEndpoint(translatePublicEndpointToSDK(model.getContainerServiceDeployment() == null ? null: model.getContainerServiceDeployment().getPublicEndpoint())).build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  public static AwsRequest translateToReadRequest(final ResourceModel model) {
    return GetContainerServicesRequest.builder().serviceName(model.getServiceName()).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  public static ResourceModel translateFromReadResponse(final AwsResponse awsResponse) {
    val getContainerServicesResponse = (GetContainerServicesResponse) awsResponse;
    if (getContainerServicesResponse == null) {
      return ResourceModel.builder().build();
    }
    val containerService = getContainerServicesResponse.containerServices().get(0);
    return translateSDKContainerServiceToResourceModel(containerService);
  }

  private static ResourceModel translateSDKContainerServiceToResourceModel(final ContainerService containerService) {
    return ResourceModel.builder().serviceName(containerService.containerServiceName()).containerArn(containerService.arn())
            .power(containerService.powerAsString()).scale(containerService.scale()).tags(translateSDKtoTag(containerService.tags()))
            .isDisabled(containerService.isDisabled()).url(containerService.url())
            .publicDomainNames(translateSDKPublicDomainNameToResourceModel(containerService.publicDomainNames()))
            .containerServiceDeployment(translateSDKContainerServiceDeploymentToResourceModel(containerService.currentDeployment()))
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  public static AwsRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteContainerServiceRequest.builder().serviceName(model.getServiceName()).build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  public static AwsRequest translateToUpdateContainerRequest(final ResourceModel model, final ResourceModel previousModel) {
    return UpdateContainerServiceRequest.builder().serviceName(model.getServiceName()).power(model.getPower()).scale(model.getScale())
            .publicDomainNames(translatePublicDomainNamesToSDK(model.getPublicDomainNames(), previousModel)).isDisabled(model.getIsDisabled()).build();
  }

  /**
   * Request to list resources
   * @return awsRequest the aws service request to list resources within aws account
   */
  public static AwsRequest translateToListRequest() {
    return GetContainerServicesRequest.builder().build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    val getContainerServicesResponse = (GetContainerServicesResponse) awsResponse;
    return getContainerServicesResponse.containerServices().stream().map(Translator::translateSDKContainerServiceToResourceModel)
            .collect(Collectors.toList());
  }

  private static EndpointRequest translatePublicEndpointToSDK(final PublicEndpoint endpoint) {
    return endpoint == null ? null : EndpointRequest.builder().containerName(endpoint.getContainerName()).containerPort(endpoint.getContainerPort())
            .healthCheck(translateHealthCheckConfigToSDK(endpoint.getHealthCheckConfig())).build();
  }

  private static ContainerServiceHealthCheckConfig translateHealthCheckConfigToSDK(final HealthCheckConfig healthCheckConfig) {
    return healthCheckConfig == null ? null : ContainerServiceHealthCheckConfig.builder().healthyThreshold(healthCheckConfig.getHealthyThreshold())
            .intervalSeconds(healthCheckConfig.getIntervalSeconds()).path(healthCheckConfig.getPath())
            .successCodes(healthCheckConfig.getSuccessCodes()).timeoutSeconds(healthCheckConfig.getTimeoutSeconds())
            .unhealthyThreshold(healthCheckConfig.getUnhealthyThreshold()).build();
  }

  private static Map<String, software.amazon.awssdk.services.lightsail.model.Container> translateContainersToSDK(final Set<Container> containers) {
    Map<String, software.amazon.awssdk.services.lightsail.model.Container> sdkContainers = new HashMap<>();
    if (containers == null) {
      return null;
    }
    for (Container container: containers) {
      sdkContainers.put(container.getContainerName(), software.amazon.awssdk.services.lightsail.model.Container.builder()
              .command(container.getCommand()).image(container.getImage()).environment(translateEnvironmentToSDK(container.getEnvironment()))
              .portsWithStrings(translatePortInfoToSDK(container.getPorts())).build());
    }
    return sdkContainers;
  }

  private static Map<String, String> translateEnvironmentToSDK(final Set<EnvironmentVariable> environments) {
    Map<String, String> sdkEnvironments = new HashMap<>();
    if (environments == null) {
      return null;
    }
    for (EnvironmentVariable environmentVariable: environments) {
      sdkEnvironments.put(environmentVariable.getVariable(), environmentVariable.getValue());
    }
    return sdkEnvironments;
  }

  private static Map<String, String> translatePortInfoToSDK(final Set<PortInfo> ports) {
    Map<String, String> sdkPorts = new HashMap<>();
    if (ports == null) {
      return null;
    }
    for (PortInfo port: ports) {
      sdkPorts.put(port.getPort(), port.getProtocol());
    }
    return sdkPorts;
  }

  private static Map<String, Set<String>> translatePublicDomainNamesToSDK(Set<PublicDomainName> domainNames, ResourceModel previousModel) {
    Map<String, Set<String>> sdkDomains = new HashMap<>();
    if (domainNames == null) {
      if (previousModel != null && previousModel.getPublicDomainNames() != null) {
        for (PublicDomainName domainName: previousModel.getPublicDomainNames()) {
          sdkDomains.put(domainName.getCertificateName(), new HashSet<>());
        }
      }
      return sdkDomains;
    }
    for (PublicDomainName domainName: domainNames) {
      sdkDomains.put(domainName.getCertificateName(), domainName.getDomainNames());
    }
    return sdkDomains;
  }

  private static ContainerServiceDeployment translateSDKContainerServiceDeploymentToResourceModel
          (final software.amazon.awssdk.services.lightsail.model.ContainerServiceDeployment containerServiceDeployment) {
    return containerServiceDeployment == null ? null : ContainerServiceDeployment.builder().containers(translateSDKContainersToResourceModel(containerServiceDeployment.containers()))
            .publicEndpoint(translateSDKPublicEndpointToResourceModel(containerServiceDeployment.publicEndpoint())).build();
  }

  private static PublicEndpoint translateSDKPublicEndpointToResourceModel
          (final software.amazon.awssdk.services.lightsail.model.ContainerServiceEndpoint endpoint) {
    return endpoint == null ? null : PublicEndpoint.builder().containerName(endpoint.containerName()).containerPort(endpoint.containerPort())
            .healthCheckConfig(translateSDKHealthCheckToResourceModel(endpoint.healthCheck())).build();
  }

  private static HealthCheckConfig translateSDKHealthCheckToResourceModel
          (final software.amazon.awssdk.services.lightsail.model.ContainerServiceHealthCheckConfig healthCheckConfig) {
    return healthCheckConfig == null ? null : HealthCheckConfig.builder().healthyThreshold(healthCheckConfig.healthyThreshold()).intervalSeconds(healthCheckConfig.intervalSeconds())
            .path(healthCheckConfig.path()).successCodes(healthCheckConfig.successCodes()).timeoutSeconds(healthCheckConfig.timeoutSeconds())
            .unhealthyThreshold(healthCheckConfig.unhealthyThreshold()).build();
  }

  private static Set<Container> translateSDKContainersToResourceModel
          (final Map<String, software.amazon.awssdk.services.lightsail.model.Container> containers) {
    Set<Container> resourceModelContainers = new HashSet<>();
    if (containers == null) {
      return null;
    }
    for (Map.Entry<String, software.amazon.awssdk.services.lightsail.model.Container> entry: containers.entrySet()) {
      resourceModelContainers.add(Container.builder().containerName(entry.getKey()).image(entry.getValue().image())
              .command(entry.getValue().command().stream().collect(Collectors.toSet()))
              .environment(translateSDKEnvironmentVariablesToResourceModel(entry.getValue().environment()))
              .ports(translateSDKPortsToResourceModel(entry.getValue().portsAsStrings())).build());
    }
    return resourceModelContainers;
  }

  private static Set<EnvironmentVariable> translateSDKEnvironmentVariablesToResourceModel
          (final Map<String, String> environments) {
    Set<EnvironmentVariable> environmentVariables = new HashSet<>();
    if (environments == null) {
      return null;
    }
    for (Map.Entry<String, String> entry: environments.entrySet()) {
      environmentVariables.add(EnvironmentVariable.builder().variable(entry.getKey()).value(entry.getValue()).build());
    }
    return environmentVariables;
  }

  private static Set<PortInfo> translateSDKPortsToResourceModel
          (final Map<String, String> ports) {
    Set<PortInfo> portInfos = new HashSet<>();
    if (ports == null) {
      return null;
    }
    for (Map.Entry<String, String> entry: ports.entrySet()) {
      portInfos.add(PortInfo.builder().port(entry.getKey()).protocol(entry.getValue()).build());
    }
    return portInfos;
  }

  private static Set<PublicDomainName> translateSDKPublicDomainNameToResourceModel(Map<String, List<String>> domainNames) {
    if (domainNames == null || domainNames.isEmpty()) {
      return null;
    }
    Set<PublicDomainName> modelDomains = new HashSet<>();
    for (Map.Entry<String, List<String>> entry: domainNames.entrySet()) {
      modelDomains.add(PublicDomainName.builder()
              .certificateName(entry.getKey()).domainNames(entry.getValue().stream().collect(Collectors.toSet())).build());
    }
    return modelDomains;
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
