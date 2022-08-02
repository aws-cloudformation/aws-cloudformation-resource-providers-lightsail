package software.amazon.lightsail.loadbalancertlscertificate.helpers.resource;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.lightsail.loadbalancertlscertificate.helpers.GetModifiedLbTlsCertResponse;

public interface ResourceHelper {
    AwsResponse update(AwsRequest request);

    AwsResponse create(AwsRequest request);

    AwsResponse delete(AwsRequest request);

    GetModifiedLbTlsCertResponse read(AwsRequest request);

    boolean isStabilizedUpdate();

    boolean isStabilizedDelete();

    boolean isSafeExceptionCreateOrUpdate(Exception e);

    boolean isSafeExceptionDelete(Exception e);
}
