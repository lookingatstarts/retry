package retry.strategy;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import retry.attempt.Attempt;

public final class WaitStrategies {

  private static final WaitStrategy NO_WAIT_STRATEGY = new FixedWaitStrategy(0L);

  private WaitStrategies() {
  }

  public static WaitStrategy noWait() {
    return NO_WAIT_STRATEGY;
  }

  public static WaitStrategy fixedWait(long sleepTime, TimeUnit timeUnit)
      throws IllegalStateException {
    if (timeUnit == null) {
      throw new IllegalArgumentException("timeUnit must not be null");
    }
    return new FixedWaitStrategy(timeUnit.toMillis(sleepTime));
  }

  public static WaitStrategy randomWait(
      long minimumTime, TimeUnit minimumTimeUnit,
      long maximumTime, TimeUnit maximumTimeUnit) {
    if (minimumTimeUnit == null) {
      throw new IllegalArgumentException("The minimum time unit may not be null");
    }
    if (maximumTimeUnit == null) {
      throw new IllegalArgumentException("The maximum time unit may not be null");
    }
    return new RandomWaitStrategy(
        minimumTimeUnit.toMillis(minimumTime),
        maximumTimeUnit.toMillis(maximumTime));
  }

  public static WaitStrategy incrementingWait(
      long initialSleepTime, TimeUnit initialSleepTimeUnit,
      long increment, TimeUnit incrementTimeUnit) {
    if (incrementTimeUnit == null) {
      throw new IllegalArgumentException("The incrementTimeUnit may not be null");
    }
    if (initialSleepTimeUnit == null) {
      throw new IllegalArgumentException("The initialSleepTimeUnit may not be null");
    }
    return new IncrementingWaitStrategy(
        initialSleepTimeUnit.toMillis(initialSleepTime),
        incrementTimeUnit.toMillis(increment));
  }

  public static WaitStrategy exponentialWait() {
    return new ExponentialWaitStrategy(1, Long.MAX_VALUE);
  }

  public static WaitStrategy exponentialWait(long maximumTime, TimeUnit maximumTimeUnit) {
    return exponentialWait(1, maximumTime, maximumTimeUnit);
  }

  public static WaitStrategy exponentialWait(long multiplier,
      long maximumTime, TimeUnit maximumTimeUnit) {
    if (maximumTimeUnit == null) {
      throw new NullPointerException("The maximum time unit may not be null");
    }
    return new ExponentialWaitStrategy(multiplier, maximumTimeUnit.toMillis(maximumTime));
  }

  public static WaitStrategy fibonacciWait() {
    return new FibonacciWaitStrategy(1, Long.MAX_VALUE);
  }

  public static WaitStrategy fibonacciWait(long maximumTime, TimeUnit maximumTimeUnit) {
    return fibonacciWait(1, maximumTime, maximumTimeUnit);
  }

  public static WaitStrategy fibonacciWait(long multiplier,
      long maximumTime, TimeUnit maximumTimeUnit) {
    if (maximumTimeUnit == null) {
      throw new IllegalArgumentException("The maximum time unit may not be null");
    }
    return new FibonacciWaitStrategy(multiplier, maximumTimeUnit.toMillis(maximumTime));
  }

  public static <T extends Throwable> WaitStrategy exceptionWait(
      Class<T> exceptionClass, Function<T, Long> function) {
    if (exceptionClass == null) {
      throw new NullPointerException("The exceptionClass may not be null");
    }
    if (function == null) {
      throw new NullPointerException("The function may not be null");
    }
    return new ExceptionWaitStrategy<T>(exceptionClass, function);
  }

  /**
   * 组合
   */
  public static WaitStrategy join(WaitStrategy... waitStrategies) {
    if (waitStrategies.length == 0) {
      throw new IllegalArgumentException("waitStrategies must contain at least one waitStrategy");
    }
    for (WaitStrategy waitStrategy : waitStrategies) {
      if (waitStrategy == null) {
        throw new IllegalArgumentException("Cannot have a null wait strategy");
      }
    }
    List<WaitStrategy> waitStrategyList = Arrays
        .stream(waitStrategies)
        .collect(Collectors.toList());
    return new CompositeWaitStrategy(waitStrategyList);
  }

  /**
   * 固定时间算法
   */
  private static final class FixedWaitStrategy implements WaitStrategy {

    private final long sleepTime;

    public FixedWaitStrategy(long sleepTime) {
      if (sleepTime < 0) {
        throw new IllegalArgumentException("The sleep time may not be negative");
      }
      this.sleepTime = sleepTime;
    }

    @Override
    public long computeSleepTime(Attempt<?> failedAttempt) {
      return sleepTime;
    }
  }


  /**
   * 在最大-最小之间随机算法
   */
  private static final class RandomWaitStrategy implements WaitStrategy {

    private static final Random RANDOM = new Random();
    private final long minimum;
    private final long maximum;

    public RandomWaitStrategy(long minimum, long maximum) {
      if (minimum < 0) {
        throw new IllegalArgumentException("minimum must be >= 0");
      }
      if (maximum <= minimum) {
        throw new IllegalArgumentException("maximum must be <= minimum");
      }
      this.minimum = minimum;
      this.maximum = maximum;
    }

    @Override
    public long computeSleepTime(Attempt<?> failedAttempt) {
      long t = Math.abs(RANDOM.nextLong()) % (maximum - minimum);
      return t + minimum;
    }
  }

  /**
   * 自增算法
   */
  private static final class IncrementingWaitStrategy implements WaitStrategy {

    private final long initialSleepTime;
    private final long increment;

    public IncrementingWaitStrategy(long initialSleepTime, long increment) {
      if (initialSleepTime < 0) {
        throw new IllegalArgumentException("initialSleepTime must be >= 0");
      }
      this.initialSleepTime = initialSleepTime;
      this.increment = increment;
    }

    @Override
    public long computeSleepTime(Attempt<?> failedAttempt) {
      long result = initialSleepTime + (increment * (failedAttempt.getAttemptTimes() - 1));
      return Math.max(result, 0L);
    }
  }

  /**
   * 指数算法
   */
  private static final class ExponentialWaitStrategy implements WaitStrategy {

    private final long multiplier;
    private final long maximumWait;

    public ExponentialWaitStrategy(long multiplier, long maximumWait) {
      if (multiplier <= 0) {
        throw new IllegalArgumentException("multiplier must be > 0");
      }
      if (maximumWait < 0) {
        throw new IllegalArgumentException("maximumWait must be > 0");
      }
      if (multiplier >= maximumWait) {
        throw new IllegalArgumentException("multiplier must be < maximumWait");
      }
      this.multiplier = multiplier;
      this.maximumWait = maximumWait;
    }

    @Override
    public long computeSleepTime(Attempt<?> failedAttempt) {
      double exp = Math.pow(2, failedAttempt.getAttemptTimes());
      long result = Math.round(multiplier * exp);
      if (result > maximumWait) {
        result = maximumWait;
      }
      return Math.max(result, 0L);
    }
  }

  /**
   * 斐波那契数列算法
   */
  private static final class FibonacciWaitStrategy implements WaitStrategy {

    private final long multiplier;
    private final long maximumWait;

    public FibonacciWaitStrategy(long multiplier, long maximumWait) {
      if (multiplier <= 0) {
        throw new IllegalArgumentException("multiplier must be > 0");
      }
      if (maximumWait < 0) {
        throw new IllegalArgumentException("maximumWait must be > 0");
      }
      if (multiplier >= maximumWait) {
        throw new IllegalArgumentException("multiplier must be < maximumWait");
      }
      this.multiplier = multiplier;
      this.maximumWait = maximumWait;
    }

    @Override
    public long computeSleepTime(Attempt<?> failedAttempt) {
      long fib = fib(failedAttempt.getAttemptTimes());
      long result = multiplier * fib;
      if (result > maximumWait || result < 0L) {
        result = maximumWait;
      }
      return Math.max(result, 0L);
    }

    /**
     * 斐波那契算法
     */
    private long fib(long n) {
      if (n == 0L) {
        return 0L;
      }
      if (n == 1L) {
        return 1L;
      }
      long prevPrev = 0L;
      long prev = 1L;
      long result = 0L;
      for (long i = 2L; i <= n; i++) {
        result = prev + prevPrev;
        prevPrev = prev;
        prev = result;
      }
      return result;
    }
  }

  /**
   * 组合策略
   */
  private static final class CompositeWaitStrategy implements WaitStrategy {

    private final List<WaitStrategy> waitStrategies;

    public CompositeWaitStrategy(List<WaitStrategy> waitStrategies) {
      if (waitStrategies.isEmpty()) {
        throw new IllegalArgumentException("waitStrategies must not be empty");
      }
      this.waitStrategies = waitStrategies;
    }

    @Override
    public long computeSleepTime(Attempt<?> failedAttempt) {
      long waitTime = 0L;
      for (WaitStrategy waitStrategy : waitStrategies) {
        waitTime += waitStrategy.computeSleepTime(failedAttempt);
      }
      return waitTime;
    }
  }


  /**
   * 根据异常配置不同的阻塞时间
   */
  private static final class ExceptionWaitStrategy<T extends Throwable> implements WaitStrategy {

    private final Class<T> exceptionClass;
    private final Function<T, Long> function;

    public ExceptionWaitStrategy(Class<T> exceptionClass, Function<T, Long> function) {
      this.exceptionClass = exceptionClass;
      this.function = function;
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions", "unchecked"})
    @Override
    public long computeSleepTime(Attempt<?> lastAttempt) {
      if (lastAttempt.hasException()) {
        Throwable cause = lastAttempt.getCause();
        if (exceptionClass.isAssignableFrom(cause.getClass())) {
          return function.apply((T) cause);
        }
      }
      return 0L;
    }
  }
}
