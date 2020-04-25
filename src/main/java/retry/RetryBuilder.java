package retry;


import retry.attempt.Attempt;
import retry.caller.AttemptCaller;
import retry.publish.RetryPublish;
import retry.strategy.StopStrategies;
import retry.strategy.StopStrategy;
import retry.strategy.WaitStrategies;
import retry.strategy.WaitStrategy;
import retry.subscribe.RetrySubscribe;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class RetryBuilder<V> {

    private AttemptCaller<V> caller;
    private RetryPublish retryPublish = new RetryPublish();
    private Set<Predicate<Attempt<V>>> rejectionPredicates = new HashSet<>();
    private StopStrategy stopStrategy = StopStrategies.neverStop();
    private WaitStrategy waitStrategy = WaitStrategies.neverWait();

    private RetryBuilder() {
    }

    public static <V> RetryBuilder<V> newBuilder() {
        return new RetryBuilder<V>();
    }

    public RetryBuilder<V> retrySubscribe(RetrySubscribe retrySubscribe) {
        this.retryPublish.addRetrySubscribe(retrySubscribe);
        return this;
    }

    public RetryBuilder<V> waitStrategy(WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
        return this;
    }

    public RetryBuilder<V> stopStrategy(StopStrategy stopStrategy) {
        this.stopStrategy = stopStrategy;
        return this;
    }

    public RetryBuilder<V> caller(AttemptCaller<V> attemptCaller) {
        this.caller = attemptCaller;
        return this;
    }

    /**
     * isAssignableFrom()方法是从类继承的角度去判断
     * instanceof关键字是从实例继承的角度去判断。
     * 父类.class.isAssignableFrom(子类.class)
     * 子类实例 instanceof 父类类型
     */
    public RetryBuilder<V> retryIfExceptionOfType(Class<? extends Throwable> exception) {
        Predicate<Attempt<V>> predicate = (attempt -> {
            if (!attempt.hasException()) {
                return false;
            }
            return exception.isAssignableFrom(attempt.getCause().getClass());
        });
        rejectionPredicates.add(predicate);
        return this;
    }

    public RetryBuilder<V> retryIfResult(Predicate<V> resultPredicate) {
        Predicate<Attempt<V>> predicate = attempt -> {
            if (!attempt.hasResult()) {
                return false;
            }
            return resultPredicate.test(attempt.getResult());
        };
        rejectionPredicates.add(predicate);
        return this;
    }

    public Retry<V> builder() {
        if (this.caller == null) {
            throw new IllegalArgumentException("caller");
        }
        this.rejectionPredicates.add(attempt -> false);
        return new Retry<>(this.caller, this.retryPublish, this.rejectionPredicates, this.stopStrategy, this.waitStrategy);
    }
}
