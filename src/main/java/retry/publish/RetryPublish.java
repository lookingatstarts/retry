package retry.publish;

import retry.attempt.Attempt;
import retry.subscribe.RetrySubscribe;

import java.util.HashSet;
import java.util.Set;

public class RetryPublish {

    private Set<RetrySubscribe> retrySubscribes = new HashSet<>();

    public synchronized void addRetrySubscribe(RetrySubscribe retryObserver) {
        retrySubscribes.add(retryObserver);
    }

    public synchronized void notifyAll(Attempt<?> attempt) {
        for (RetrySubscribe retryObserver : retrySubscribes) {
            retryObserver.onRetry(attempt);
        }
    }
}
