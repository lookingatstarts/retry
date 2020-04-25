package retry;

import retry.attempt.Attempt;
import retry.attempt.ExceptionAttempt;
import retry.attempt.ResultAttempt;
import retry.caller.AttemptCaller;
import retry.exception.RetryException;
import retry.publish.RetryPublish;
import retry.strategy.StopStrategy;
import retry.strategy.WaitStrategy;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class Retry<V> {

    private AttemptCaller<V> caller;
    private RetryPublish retryPublish;
    private Set<Predicate<Attempt<V>>> rejectionPredicates;
    private StopStrategy stopStrategy;
    private WaitStrategy waitStrategy;

    public Retry(AttemptCaller<V> caller, RetryPublish retryPublish, Set<Predicate<Attempt<V>>> rejectionPredicates, StopStrategy stopStrategy, WaitStrategy waitStrategy) {
        this.caller = caller;
        this.retryPublish = retryPublish;
        this.rejectionPredicates = rejectionPredicates;
        this.stopStrategy = stopStrategy;
        this.waitStrategy = waitStrategy;
    }

    public V call(Callable<V> callable) throws ExecutionException {
        // 开始执行时间
        long startTime = System.nanoTime();
        for (int attemptTimes = 1; ; attemptTimes++) {
            Attempt<V> attempt;
            try {
                // 同步获取结果
                V result = caller.call(callable);
                attempt = new ResultAttempt<>(result, attemptTimes, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
            } catch (Throwable t) {
                if (t instanceof ExecutionException) {
                    t = t.getCause();
                }
                attempt = new ExceptionAttempt<>(t, attemptTimes, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
            }
            retryPublish.notifyAll(attempt);
            boolean shouldRetry = false;
            for (Predicate<Attempt<V>> rejectionPredicate : rejectionPredicates) {
                if (shouldRetry) {
                    break;
                }
                shouldRetry = rejectionPredicate.test(attempt);
            }
            if (!shouldRetry) {
                return attempt.get();
            }
            // 结束运行
            if (stopStrategy.shouldStop(attempt)) {
                throw new RetryException(attempt);
            }
            // 等待下次运行
            final long sleepTime = waitStrategy.nextSleepTime(attempt);
            if (sleepTime <= 0) {
                continue;
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RetryException(attempt);
            }
        }
    }
}
