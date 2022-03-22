package software.amazon.lightsail.distribution;

import software.amazon.cloudformation.proxy.StdCallbackContext;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {

    public static final Constant BACKOFF_DELAY = Constant.of().delay(Duration.ofSeconds(30))
            .timeout(Duration.ofMinutes(25)).build();

    public static String PRE_CHECK_CREATE = "preCheckCreate";
    public static String PRE_CHECK_DELETE = "preCheckDelete";

    private Map<String, Boolean> isPreCheckDone = new HashMap<>();

    private Map<String, Integer> waitCount = new HashMap<>();

    private Integer maxWaitCount = 40;

    public int getWaitCount(final String key) {
        return this.waitCount.getOrDefault(key, 0);
    }

    public void incrementWaitCount(final String key) {
        this.waitCount.put(key, getWaitCount(key) + 1);
    }

    public boolean isWaitCountReached(final String key) {
        return this.getWaitCount(key) > this.maxWaitCount;
    }

    public boolean getIsPreCheckDone(final String key) {
        return this.isPreCheckDone.getOrDefault(key, false);
    }

}
