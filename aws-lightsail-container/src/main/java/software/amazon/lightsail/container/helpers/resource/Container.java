package software.amazon.lightsail.container.helpers.resource;

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
import software.amazon.lightsail.container.EnvironmentVariable;
import software.amazon.lightsail.container.HealthCheckConfig;
import software.amazon.lightsail.container.ResourceModel;
import software.amazon.lightsail.container.Translator;

import java.util.*;

/**
 * Helper class to handle Container operations.
 */
@RequiredArgsConstructor
public class Container implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    public AwsResponse update(AwsRequest request) {
        val updateRequest = Translator.translateToUpdateContainerRequest(resourceModel, resourceModelRequest.getPreviousResourceState());
        AwsResponse awsResponse;
        logger.log(String.format("Updating Container: %s", resourceModel.getServiceName()));
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((UpdateContainerServiceRequest) updateRequest),
                proxyClient.client()::updateContainerService);
        logger.log(String.format("Successfully updated Container: %s", resourceModel.getServiceName()));
        return awsResponse;
    }

    @Override
    public AwsResponse create(AwsRequest request) {
        logger.log(String.format("Creating Container: %s", resourceModel.getServiceName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((CreateContainerServiceRequest) request),
                proxyClient.client()::createContainerService);
        logger.log(String.format("Successfully created Container: %s", resourceModel.getServiceName()));
        return awsResponse;
    }

    public AwsResponse createContainerServiceDeployment(AwsRequest request) {
        if (!isDeploymentRequired()) {
            logger.log(String.format("Container Service Deployment not required for: %s", resourceModel.getServiceName()));
            return CreateContainerServiceDeploymentResponse.builder().build();
        }
        logger.log(String.format("Creating Container Service Deployment for: %s", resourceModel.getServiceName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((CreateContainerServiceDeploymentRequest) request),
                proxyClient.client()::createContainerServiceDeployment);
        logger.log(String.format("Successfully finished Container Service Deployment: %s", resourceModel.getServiceName()));
        return awsResponse;
    }

    private boolean isDeploymentRequired() {
        val desiredDeployment = resourceModelRequest.getDesiredResourceState().getContainerServiceDeployment();
        if (desiredDeployment == null) {
            return false;
        }
        val currentDeployment = ((GetContainerServicesResponse) this.read(GetContainerServicesRequest.builder()
                .serviceName(resourceModel.getServiceName()).build())).containerServices().get(0).currentDeployment();
        logger.log(desiredDeployment.toString());
        logger.log(currentDeployment == null ? "null" : currentDeployment.toString());
        if (currentDeployment == null) {
            return true;
        }

        // Checking containers.
        if (desiredDeployment.getContainers().size() != currentDeployment.containers().size()) {
            return true;
        }
        for (val desiredContainer: desiredDeployment.getContainers()) {
            if (currentDeployment.containers() != null && currentDeployment.containers().containsKey(desiredContainer.getContainerName())) {
                val currentContainer = currentDeployment.containers().get(desiredContainer.getContainerName());
                if (!currentContainer.image().equals(desiredContainer.getImage()) ||
                        isCommandUpdated(desiredContainer.getCommand(), currentContainer.command()) ||
                        isEnvironmentUpdated(desiredContainer.getEnvironment(), currentContainer.environment()) ||
                        isPortUpdated(desiredContainer.getPorts(), currentContainer.portsAsStrings())) {
                    return true;
                }
            } else {
                return true;
            }
        }

        val currentEndpoint = currentDeployment.publicEndpoint();
        val desiredEndpoint = desiredDeployment.getPublicEndpoint();
        // Checking EndpointRequest
        if (currentEndpoint != null && desiredEndpoint != null) {
            if (!currentEndpoint.containerName().equals(desiredEndpoint.getContainerName()) ||
                    currentEndpoint.containerPort() != desiredEndpoint.getContainerPort() ||
                    isHealthCheckUpdated(desiredEndpoint.getHealthCheckConfig(), currentEndpoint.healthCheck())) {
                return true;
            }
        } else if (currentEndpoint == null && desiredEndpoint == null) {
            return false;
        } else {
            return true;
        }

        return false;
    }

    private boolean isHealthCheckUpdated(HealthCheckConfig desiredHealthCheck, ContainerServiceHealthCheckConfig currentHealthCheck) {
        if (currentHealthCheck != null && desiredHealthCheck != null) {
            if (desiredHealthCheck.getHealthyThreshold() != currentHealthCheck.healthyThreshold() ||
                    desiredHealthCheck.getIntervalSeconds() != currentHealthCheck.intervalSeconds() ||
                    (desiredHealthCheck.getPath() != null && !desiredHealthCheck.getPath().equals(currentHealthCheck.path())) ||
                    (desiredHealthCheck.getSuccessCodes() != null && !desiredHealthCheck.getSuccessCodes().equals(currentHealthCheck.successCodes())) ||
                    desiredHealthCheck.getTimeoutSeconds() != currentHealthCheck.timeoutSeconds() ||
                    desiredHealthCheck.getUnhealthyThreshold() != currentHealthCheck.unhealthyThreshold()) {
                return true;
            } else {
                return false;
            }
        } else if (currentHealthCheck == null && desiredHealthCheck == null) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isCommandUpdated(Set<String> desiredCommand, List<String> currentCommand) {
        if (currentCommand != null && desiredCommand != null) {
            return !(desiredCommand.containsAll(currentCommand) && currentCommand.containsAll(desiredCommand));
        } else if (currentCommand == desiredCommand) {
            return false;
        } else if ((currentCommand != null && currentCommand.size() == 0) || (desiredCommand != null && desiredCommand.size() == 0)) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isEnvironmentUpdated(Set<EnvironmentVariable> desiredEnvironment, Map<String, String> currentEnvironment) {
        if (currentEnvironment != null && desiredEnvironment != null) {
            if (currentEnvironment.size() != desiredEnvironment.size()) {
                return true;
            }
            for (EnvironmentVariable environment: desiredEnvironment) {
                if (!currentEnvironment.containsKey(environment.getVariable())) {
                    return true;
                } else {
                    if (!environment.getValue().equals(currentEnvironment.get(environment.getVariable()))) {
                        return true;
                    }
                }
            }
            return false;
        } else if (currentEnvironment == desiredEnvironment) {
            return false;
        } else if ((currentEnvironment != null && currentEnvironment.size() == 0) || (desiredEnvironment != null && desiredEnvironment.size() == 0)) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isPortUpdated(Set<software.amazon.lightsail.container.PortInfo> desiredPorts, Map<String, String> currentPorts) {
        if (currentPorts != null && desiredPorts != null) {
            if (currentPorts.size() != desiredPorts.size()) {
                return true;
            }
            for (software.amazon.lightsail.container.PortInfo portInfo: desiredPorts) {
                if (!currentPorts.containsKey(portInfo.getPort())) {
                    return true;
                } else {
                    if (!portInfo.getProtocol().equals(currentPorts.get(portInfo.getPort()))) {
                        return true;
                    }
                }
            }
            return false;
        } else if (currentPorts == desiredPorts) {
            return false;
        } else if ((currentPorts != null && currentPorts.size() == 0) || (desiredPorts != null && desiredPorts.size() == 0)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public AwsResponse delete(AwsRequest request) {
        logger.log(String.format("Deleting Container: %s", resourceModel.getServiceName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((DeleteContainerServiceRequest) request),
                proxyClient.client()::deleteContainerService);
        logger.log(String.format("Successfully deleted Container: %s", resourceModel.getServiceName()));
        return awsResponse;
    }

    /**
     * Read Container.
     *
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse read(AwsRequest request) {
        val containerName = ((GetContainerServicesRequest) request).serviceName();
        logger.log(String.format("Reading Container: %s", containerName));
        return proxyClient.injectCredentialsAndInvokeV2(GetContainerServicesRequest.builder().serviceName(containerName).build(),
                proxyClient.client()::getContainerServices);
    }

    @Override
    public boolean isStabilizedUpdate() {
        val awsResponse = ((GetContainerServicesResponse) this
                .read(GetContainerServicesRequest.builder().serviceName(resourceModel.getServiceName()).build()));
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Container: %s has stabilized. Current state: %s",
                resourceModel.getServiceName(), currentState));
        return ("Running".equalsIgnoreCase(currentState) || "Ready".equalsIgnoreCase(currentState) || "Disabled".equalsIgnoreCase(currentState));
    }

    public boolean isStabilizedCreate() {
        val awsResponse = ((GetContainerServicesResponse) this
                .read(GetContainerServicesRequest.builder().serviceName(resourceModel.getServiceName()).build()));
        val currentState = getCurrentState(awsResponse);
        logger.log(String.format("Checking if Container: %s has stabilized. Current state: %s",
                resourceModel.getServiceName(), currentState));
        return ("Ready".equalsIgnoreCase(currentState));
    }

    @Override
    public boolean isStabilizedDelete() {
        final boolean stabilized = false;
        logger.log(String.format("Checking if Container: %s deletion has stabilized.",
                resourceModel.getServiceName(), stabilized));
        try {
            this.read(GetContainerServicesRequest.builder().serviceName(resourceModel.getServiceName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            logger.log(String.format("Container: %s deletion has stabilized", resourceModel.getServiceName()));
            return true;
        }
        return stabilized;
    }

    /**
     * Get Current state of the Container.
     *
     * @return
     *
     * @param awsResponse
     */
    private String getCurrentState(GetContainerServicesResponse awsResponse) {
        val containerService = awsResponse.containerServices().get(0);
        return containerService.state() == null ? "Pending" : containerService.state().toString();
    }

    @Override
    public boolean isSafeExceptionCreateOrUpdate(Exception e) {
        return false;
    }

    @Override
    public boolean isSafeExceptionDelete(Exception e) {
        if (e instanceof CfnNotFoundException || e instanceof NotFoundException || e instanceof InvalidInputException) {
            return true; // Its stabilized if the resource is gone..
        }
        return false;
    }
}
