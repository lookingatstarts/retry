package retry;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import retry.attempt.Attempt;
import retry.attempt.ExceptionAttempt;
import retry.attempt.ResultAttempt;
import retry.caller.AttemptCaller;
import retry.exception.RetryException;
import retry.publish.RetryObservable;
import retry.strategy.BlockStrategy;
import retry.strategy.StopStrategy;
import retry.strategy.WaitStrategy;

public class Retryer<V> {

  private final AttemptCaller<V> caller;
  private final RetryObservable retryObservable;
  private final Predicate<Attempt<V>> attemptPredicate;
  private final StopStrategy stopStrategy;
  private final WaitStrategy waitStrategy;
  private final BlockStrategy blockStrategy;

  public Retryer(AttemptCaller<V> caller, RetryObservable retryObservable,
      Predicate<Attempt<V>> attemptPredicate, StopStrategy stopStrategy,
      WaitStrategy waitStrategy, BlockStrategy blockStrategy) {
    this.caller = caller;
    this.retryObservable = retryObservable;
    this.attemptPredicate = attemptPredicate;
    this.stopStrategy = stopStrategy;
    this.waitStrategy = waitStrategy;
    this.blockStrategy = blockStrategy;
  }

  public V call(Callable<V> callable) throws ExecutionException {
    // 开始执行时间
    long startTime = System.nanoTime();
    for (int attemptTimes = 1; ; attemptTimes++) {
      Attempt<V> attempt;
      try {
        // 同步获取结果
        V result = caller.call(callable);
        attempt = new ResultAttempt<>(result, attemptTimes,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
      } catch (Throwable t) {
        if (t instanceof ExecutionException) {
          t = t.getCause();
        }
        attempt = new ExceptionAttempt<>(t, attemptTimes,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
      }
      // 通知
      retryObservable.notifyAll(attempt);
      if (!attemptPredicate.test(attempt)) {
        return attempt.get();
      }
      // 结束运行
      if (stopStrategy.shouldStop(attempt)) {
        throw new RetryException(attempt);
      }
      // 等待下次运行
      final long sleepTime = waitStrategy.computeSleepTime(attempt);
      if (sleepTime <= 0) {
        continue;
      }
      try {
        blockStrategy.block(sleepTime);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RetryException(attempt);
      }
    }
  }
}
