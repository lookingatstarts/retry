package retry.strategy;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import retry.attempt.Attempt;

public final class StopStrategies {

  private static final StopStrategy NEVER_STOP = new NeverStopStrategy();

  private StopStrategies() {
  }

  public static StopStrategy neverStop() {
    return NEVER_STOP;
  }

  public static StopStrategy stopAfterAttempt(int attemptNumber) {
    return new StopAfterAttemptStrategy(attemptNumber);
  }

  public static StopStrategy stopAfterDelay(long duration, TimeUnit timeUnit) {
    if (Objects.isNull(timeUnit)) {
      throw new IllegalArgumentException("TimeUnit must not be null");
    }
    return new StopAfterDelayStrategy(timeUnit.toMillis(duration));
  }

  /**
   * 不重试
   */
  private static final class NeverStopStrategy implements StopStrategy {

    @Override
    public boolean shouldStop(Attempt<?> failedAttempt) {
      return false;
    }
  }

  /**
   * 最大次数重试
   */
  private static final class StopAfterAttemptStrategy implements StopStrategy {

    private final int maxAttemptNumber;

    public StopAfterAttemptStrategy(int maxAttemptNumber) {
      if (maxAttemptNumber < 1) {
        throw new IllegalArgumentException("maxAttemptNumber must be greater than 0");
      }
      this.maxAttemptNumber = maxAttemptNumber;
    }

    @Override
    public boolean shouldStop(Attempt<?> failedAttempt) {
      return failedAttempt.getAttemptTimes() >= maxAttemptNumber;
    }
  }

  /**
   * 超时策略
   */
  private static final class StopAfterDelayStrategy implements StopStrategy {

    private final long maxDelay;

    public StopAfterDelayStrategy(long maxDelay) {
      if (maxDelay < 0) {
        throw new IllegalArgumentException("maxDelay must be greater than 0");
      }
      this.maxDelay = maxDelay;
    }

    @Override
    public boolean shouldStop(Attempt<?> failedAttempt) {
      return failedAttempt.getDelaySinceFirstAttempt() >= maxDelay;
    }
  }
}
