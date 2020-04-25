package retry.subscribe;

import retry.attempt.Attempt;

/**
 * 重试订阅者，执行完一次调用就会调用onRetry方法
 */
@FunctionalInterface
public interface RetrySubscribe {

    <V> void onRetry(Attempt<V> attempt);
}
