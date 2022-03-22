package software.amazon.lightsail.distribution.helpers.resource;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;

public interface ResourceHelper {
    AwsResponse update(AwsRequest request);

    AwsResponse create(AwsRequest request);

    AwsResponse delete(AwsRequest request);

    AwsResponse read(AwsRequest request);

    boolean isStabilizedUpdate();

    boolean isStabilizedDelete();

    boolean isSafeExceptionCreateOrUpdate(Exception e);

    boolean isSafeExceptionDelete(Exception e);
}
