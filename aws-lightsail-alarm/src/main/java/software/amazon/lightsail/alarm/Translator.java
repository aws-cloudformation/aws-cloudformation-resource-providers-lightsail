package software.amazon.lightsail.alarm;

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
    return PutAlarmRequest.builder().alarmName(model.getAlarmName()).metricName(model.getMetricName())
            .monitoredResourceName(model.getMonitoredResourceName()).comparisonOperator(model.getComparisonOperator())
            .threshold(model.getThreshold()).evaluationPeriods(model.getEvaluationPeriods()).datapointsToAlarm(model.getDatapointsToAlarm())
            .treatMissingData(model.getTreatMissingData()).notificationEnabled(model.getNotificationEnabled())
            .notificationTriggers(model.getNotificationTriggers() == null ? null : model.getNotificationTriggers().stream().map(trigger -> AlarmState.fromValue(trigger)).collect(Collectors.toSet()))
            .contactProtocols(model.getContactProtocols() == null ? null : model.getContactProtocols().stream().map(protocol -> ContactProtocol.fromValue(protocol)).collect(Collectors.toSet()))
            .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  public static AwsRequest translateToReadRequest(final ResourceModel model) {
    return GetAlarmsRequest.builder().alarmName(model.getAlarmName()).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final AwsResponse awsResponse) {
    val getAlarmsResponse = (GetAlarmsResponse) awsResponse;
    if (getAlarmsResponse == null) {
      return ResourceModel.builder().build();
    }
    val alarm = getAlarmsResponse.alarms().get(0);
    return translateSDKAlarmToResourceModel(alarm);
  }

  private static ResourceModel translateSDKAlarmToResourceModel(final Alarm alarm) {
    return ResourceModel.builder().alarmName(alarm.name()).alarmArn(alarm.arn()).comparisonOperator(alarm.comparisonOperatorAsString())
            .monitoredResourceName(alarm.monitoredResourceInfo().name()).metricName(alarm.metricNameAsString())
            .threshold(alarm.threshold()).evaluationPeriods(alarm.evaluationPeriods()).datapointsToAlarm(alarm.datapointsToAlarm())
            .treatMissingData(alarm.treatMissingDataAsString()).notificationEnabled(alarm.notificationEnabled())
            .contactProtocols(alarm.contactProtocols().stream().map(protocol ->
                    protocol.name().equalsIgnoreCase("EMAIL") ? "Email" : protocol.name()).collect(Collectors.toSet()))
            .notificationTriggers(alarm.notificationTriggers().stream().map(trigger -> trigger.name()).collect(Collectors.toSet()))
            .state(alarm.stateAsString()).build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  public static AwsRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteAlarmRequest.builder().alarmName(model.getAlarmName()).build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  public static AwsRequest translateToUpdateRequest(final ResourceModel model) {
    return PutAlarmRequest.builder().alarmName(model.getAlarmName()).metricName(model.getMetricName())
            .monitoredResourceName(model.getMonitoredResourceName()).comparisonOperator(model.getComparisonOperator())
            .threshold(model.getThreshold()).evaluationPeriods(model.getEvaluationPeriods()).datapointsToAlarm(model.getDatapointsToAlarm())
            .treatMissingData(model.getTreatMissingData()).notificationEnabled(model.getNotificationEnabled())
            .notificationTriggers(model.getNotificationTriggers() == null ? null : model.getNotificationTriggers().stream().map(trigger -> AlarmState.fromValue(trigger)).collect(Collectors.toSet()))
            .contactProtocols(model.getContactProtocols() == null ? null : model.getContactProtocols().stream().map(protocol -> ContactProtocol.fromValue(protocol)).collect(Collectors.toSet()))
            .build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static AwsRequest translateToListRequest(final String nextToken) {
    return GetAlarmsRequest.builder().pageToken(nextToken).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    val getAlarmsResponse = (GetAlarmsResponse) awsResponse;
    return getAlarmsResponse.alarms().stream().map(Translator::translateSDKAlarmToResourceModel)
            .collect(Collectors.toList());
  }

}
