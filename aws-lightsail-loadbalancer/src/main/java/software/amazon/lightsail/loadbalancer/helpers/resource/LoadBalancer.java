package software.amazon.lightsail.loadbalancer.helpers.resource;

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
import software.amazon.lightsail.loadbalancer.ResourceModel;

import java.util.HashSet;
import java.util.Set;

import static software.amazon.lightsail.loadbalancer.Translator.translateFromReadResponse;

/**
 * Helper class to handle LoadBalancer operations.
 */
@RequiredArgsConstructor
public class LoadBalancer implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    public AwsResponse update(AwsRequest request) {
        AwsResponse awsResponse = null;

        return awsResponse;
    }

    public AwsResponse updateAttributes(AwsRequest request) {
        AwsResponse awsResponse = null;
        ResourceModel currentResourceModel = getCurrentResourceModelFromLightsail();
        ResourceModel desiredResourceModel = resourceModelRequest.getDesiredResourceState();

        // Updating the HealthCheckPath attribute.
        if (desiredResourceModel.getHealthCheckPath() != null &&
                !desiredResourceModel.getHealthCheckPath().equals(currentResourceModel.getHealthCheckPath())) {
            logger.log("Updating LoadBalancer attribute: HealthCheckPath");
            proxyClient.injectCredentialsAndInvokeV2(UpdateLoadBalancerAttributeRequest.builder()
                    .loadBalancerName(desiredResourceModel.getLoadBalancerName()).attributeName("HealthCheckPath")
                    .attributeValue(desiredResourceModel.getHealthCheckPath()).build(), proxyClient.client()::updateLoadBalancerAttribute);
        }

        // Updating the SessionStickinessEnabled attribute.
        if (desiredResourceModel.getSessionStickinessEnabled() == null) {
            if (currentResourceModel.getSessionStickinessEnabled()) {
                logger.log("Updating LoadBalancer attribute: SessionStickinessEnabled");
                proxyClient.injectCredentialsAndInvokeV2(UpdateLoadBalancerAttributeRequest.builder()
                        .loadBalancerName(desiredResourceModel.getLoadBalancerName()).attributeName("SessionStickinessEnabled")
                        .attributeValue("false").build(), proxyClient.client()::updateLoadBalancerAttribute);
            }
        } else {
            if (currentResourceModel.getSessionStickinessEnabled() != desiredResourceModel.getSessionStickinessEnabled()) {
                logger.log("Updating LoadBalancer attribute: SessionStickinessEnabled");
                proxyClient.injectCredentialsAndInvokeV2(UpdateLoadBalancerAttributeRequest.builder()
                        .loadBalancerName(desiredResourceModel.getLoadBalancerName()).attributeName("SessionStickinessEnabled")
                        .attributeValue(String.valueOf(desiredResourceModel.getSessionStickinessEnabled()))
                        .build(), proxyClient.client()::updateLoadBalancerAttribute);
            }
        }

        // Updating the SessionStickiness_LB_CookieDurationSeconds attribute.
        if (desiredResourceModel.getSessionStickinessLBCookieDurationSeconds() != null) {
            if (!desiredResourceModel.getSessionStickinessLBCookieDurationSeconds().equals(currentResourceModel.getSessionStickinessLBCookieDurationSeconds())) {
                logger.log("Updating LoadBalancer attribute: SessionStickiness_LB_CookieDurationSeconds");
                proxyClient.injectCredentialsAndInvokeV2(UpdateLoadBalancerAttributeRequest.builder()
                        .loadBalancerName(desiredResourceModel.getLoadBalancerName()).attributeName("SessionStickiness_LB_CookieDurationSeconds")
                        .attributeValue(desiredResourceModel.getSessionStickinessLBCookieDurationSeconds())
                        .build(), proxyClient.client()::updateLoadBalancerAttribute);
            }
        }

        return awsResponse;
    }

    public AwsResponse detachInstances(AwsRequest request) {
        AwsResponse awsResponse = DetachInstancesFromLoadBalancerResponse.builder().build();
        Set<String> desiredInstances = resourceModelRequest.getDesiredResourceState().getAttachedInstances();
        Set<String> currentInstances = getCurrentResourceModelFromLightsail().getAttachedInstances();

        Set<String> instancesToDetach = setDifference(currentInstances, desiredInstances);
        logger.log("Instances to detach: " + instancesToDetach.toString());

        if (instancesToDetach.size() == 0) {
            return awsResponse;
        }

        val detachInstancesFromLoadBalancerRequest = DetachInstancesFromLoadBalancerRequest.builder()
                .loadBalancerName(resourceModel.getLoadBalancerName()).instanceNames(instancesToDetach).build();
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(detachInstancesFromLoadBalancerRequest,
                proxyClient.client()::detachInstancesFromLoadBalancer);

        return awsResponse;
    }

    public AwsResponse attachInstances(AwsRequest request) {
        AwsResponse awsResponse = AttachInstancesToLoadBalancerResponse.builder().build();
        Set<String> desiredInstances = resourceModelRequest.getDesiredResourceState().getAttachedInstances();
        Set<String> currentInstances = getCurrentResourceModelFromLightsail().getAttachedInstances();

        Set<String> instancesToAttach = setDifference(desiredInstances, currentInstances);
        logger.log("Instances to attach: " + instancesToAttach.toString());

        if (instancesToAttach.size() == 0) {
            return awsResponse;
        }

        val attachInstancesToLoadBalancerRequest = AttachInstancesToLoadBalancerRequest.builder()
                .loadBalancerName(resourceModel.getLoadBalancerName()).instanceNames(instancesToAttach).build();
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(attachInstancesToLoadBalancerRequest,
                proxyClient.client()::attachInstancesToLoadBalancer);

        return awsResponse;
    }

    @Override
    public AwsResponse create(AwsRequest request) {
        logger.log(String.format("Creating LoadBalancer: %s", resourceModel.getLoadBalancerName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((CreateLoadBalancerRequest) request),
                proxyClient.client()::createLoadBalancer);
        logger.log(String.format("Successfully created LoadBalancer: %s", resourceModel.getLoadBalancerName()));
        return awsResponse;
    }

    @Override
    public AwsResponse delete(AwsRequest request) {
        logger.log(String.format("Deleting LoadBalancer: %s", resourceModel.getLoadBalancerName()));
        AwsResponse awsResponse = null;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((DeleteLoadBalancerRequest) request),
                proxyClient.client()::deleteLoadBalancer);
        logger.log(String.format("Successfully deleted LoadBalancer: %s", resourceModel.getLoadBalancerName()));
        return awsResponse;
    }

    /**
     * Read LoadBalancer.
     *
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse read(AwsRequest request) {
        val loadBalancerName = ((GetLoadBalancerRequest) request).loadBalancerName();
        logger.log(String.format("Reading LoadBalancer: %s", loadBalancerName));
        return proxyClient.injectCredentialsAndInvokeV2(GetLoadBalancerRequest.builder()
                .loadBalancerName(loadBalancerName).build(), proxyClient.client()::getLoadBalancer);
    }

    @Override
    public boolean isStabilizedDelete() {
        final boolean stabilized = false;
        logger.log(String.format("Checking if LoadBalancer: %s deletion has stabilized.",
                resourceModel.getLoadBalancerName(), stabilized));
        try {
            this.read(GetLoadBalancerRequest.builder().loadBalancerName(resourceModel.getLoadBalancerName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            logger.log(String.format("LoadBalancer: %s deletion has stabilized", resourceModel.getLoadBalancerName()));
            return true;
        }
        return stabilized;
    }

    public boolean isStabilizedInstances() {
        val awsResponse = ((GetLoadBalancerResponse) this
                .read(GetLoadBalancerRequest.builder().loadBalancerName(resourceModel.getLoadBalancerName()).build()));
        for (InstanceHealthSummary instanceHealthSummary: awsResponse.loadBalancer().instanceHealthSummary()) {
            if (instanceHealthSummary.instanceHealth() == InstanceHealthState.INITIAL || instanceHealthSummary.instanceHealth() == InstanceHealthState.DRAINING) {
                return false;
            }
        }
        return true;
    }

    public ResourceModel getCurrentResourceModelFromLightsail() {
        return translateFromReadResponse(this.read(GetLoadBalancerRequest.builder()
                .loadBalancerName(resourceModel.getLoadBalancerName()).build()));
    }

    public Set<String> setDifference(Set<String> setOne, Set<String> setTwo) {
        if (setOne == null || setOne.size() == 0) {
            return new HashSet<>();
        }
        if (setTwo == null || setTwo.size() == 0) {
            return setOne == null ? new HashSet<>() : setOne;
        }
        Set<String> result = new HashSet<String>(setOne);
        result.removeIf(setTwo::contains);
        return result;
    }

    /**
     * Get Current state of the LoadBalancer.
     *
     * @return
     *
     * @param awsResponse
     */
    private String getCurrentState(GetLoadBalancerResponse awsResponse) {
        val loadBalancer = awsResponse.loadBalancer();
        return loadBalancer.state() == null ? "Pending" : loadBalancer.state().name();
    }

    @Override
    public boolean isStabilizedUpdate() {
        return false;
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

}
