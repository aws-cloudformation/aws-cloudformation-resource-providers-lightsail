package software.amazon.lightsail.alarm.helpers.resource;

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
import software.amazon.lightsail.alarm.ResourceModel;

/**
 * Helper class to handle Alarm operations.
 */
@RequiredArgsConstructor
public class Alarm implements ResourceHelper {

    private final ResourceModel resourceModel;
    private final Logger logger;
    private final ProxyClient<LightsailClient> proxyClient;
    private final ResourceHandlerRequest<ResourceModel> resourceModelRequest;

    @Override
    public AwsResponse update(AwsRequest request) {
        logger.log(String.format("Updating Alarm: %s", resourceModel.getAlarmName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((PutAlarmRequest) request),
                proxyClient.client()::putAlarm);
        logger.log(String.format("Successfully updated Alarm: %s", resourceModel.getAlarmName()));
        return awsResponse;
    }

    @Override
    public AwsResponse create(AwsRequest request) {
        logger.log(String.format("Creating Alarm: %s", resourceModel.getAlarmName()));
        AwsResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((PutAlarmRequest) request),
                proxyClient.client()::putAlarm);
        logger.log(String.format("Successfully created Alarm: %s", resourceModel.getAlarmName()));
        return awsResponse;
    }

    @Override
    public AwsResponse delete(AwsRequest request) {
        logger.log(String.format("Deleting Alarm: %s", resourceModel.getAlarmName()));
        AwsResponse awsResponse = null;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(((DeleteAlarmRequest) request),
                proxyClient.client()::deleteAlarm);
        logger.log(String.format("Successfully deleted Alarm: %s", resourceModel.getAlarmName()));
        return awsResponse;
    }

    /**
     * Read Alarm.
     *
     * @param request
     *
     * @return AwsResponse
     */
    @Override
    public AwsResponse read(AwsRequest request) {
        val alarmName = resourceModel.getAlarmName();
        logger.log(String.format("Reading Alarm: %s", alarmName));
        return proxyClient.injectCredentialsAndInvokeV2(GetAlarmsRequest.builder()
                .alarmName(alarmName).build(), proxyClient.client()::getAlarms);
    }

    @Override
    public boolean isStabilizedDelete() {
        final boolean stabilized = false;
        logger.log(String.format("Checking if Alarm: %s deletion has stabilized.",
                resourceModel.getAlarmName(), stabilized));
        try {
            this.read(GetAlarmsRequest.builder().alarmName(resourceModel.getAlarmName()).build());
        } catch (final Exception e) {
            if (!isSafeExceptionDelete(e)) {
                throw e;
            }
            logger.log(String.format("Alarm: %s deletion has stabilized", resourceModel.getAlarmName()));
            return true;
        }
        return stabilized;
    }

    public boolean isStabilizedCreate() {
        logger.log(String.format("Checking if Alarm: %s creation has been stabilized.",
                resourceModel.getAlarmName()));
        try {
            this.read(GetAlarmsRequest.builder().alarmName(resourceModel.getAlarmName()).build());
        } catch (final Exception e) {
            if (e instanceof CfnNotFoundException || e instanceof NotFoundException) {
                return false;
            } else {
                throw e;
            }
        }
        logger.log(String.format("Alarm: %s creation has stabilized", resourceModel.getAlarmName()));
        return true;
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
