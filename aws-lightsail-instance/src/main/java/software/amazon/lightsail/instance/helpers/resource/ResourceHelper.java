package software.amazon.lightsail.instance.helpers.resource;

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

    /**
     * This sleep to avoid eventual consistency issues. In Get after some update.
     * Time is hardcoded here. We wanted to minimal sleep only.
     */
    static void sleepToAvoidEventualConsistency() {
        try {
            Thread.sleep(10000); //10 seconds
        } catch (InterruptedException e) {
            // pass
            // Do nothing when the sleep is skipped. We will find in the Cloudformation times.
        }
    }
}
