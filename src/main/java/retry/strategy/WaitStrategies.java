package retry.strategy;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class WaitStrategies {

    private static final Random RANDOM = new Random();

    private WaitStrategies() {
    }

    public static WaitStrategy neverWait() {
        return failedAttempt -> 0L;
    }

    public static WaitStrategy fixedWaitStrategy(long sleepTime, TimeUnit timeUnit) {
        return failedAttempt -> timeUnit.toMillis(sleepTime);
    }

    public static WaitStrategy randomWaitStrategy(long minimum, long maximum, TimeUnit timeUnit) {
        return failedAttempt -> {
            long t = Math.abs(RANDOM.nextLong()) % (maximum - minimum);
            return timeUnit.toMillis(t + minimum);
        };
    }

    public static WaitStrategy compositeWaitStrategy(List<WaitStrategy> waitStrategies) {
        return failedAttempt -> waitStrategies.stream().map(waitStrategy -> waitStrategy.nextSleepTime(failedAttempt)).count();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> WaitStrategy exceptionWaitStrategy(Function<T, Long> exceptionFunction) {
        return failedAttempt -> {
            if (failedAttempt.hasException()) {
                final Throwable cause = failedAttempt.getCause();
                return exceptionFunction.apply((T) cause);
            }
            return 0L;
        };
    }
}
