package retry.strategy;

import java.util.concurrent.TimeUnit;

public final class StopStrategies {

    private StopStrategies() {

    }

    public static StopStrategy neverStop() {
        return failedAttempt -> false;
    }

    public static StopStrategy stopAfterDelay(long timeout, TimeUnit timeUnit) {
        return failedAttempt -> timeUnit.toMillis(timeout) <= failedAttempt.getDelaySinceFirstAttempt();
    }

    public static StopStrategy stopAfterAttempt(long times) {
        return failedAttempt -> times <= failedAttempt.getAttemptTimes();
    }
}
