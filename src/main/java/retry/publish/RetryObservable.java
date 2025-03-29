package retry.publish;

import java.util.HashSet;
import java.util.Set;
import retry.attempt.Attempt;
import retry.subscribe.RetrySubscribe;

/**
 * 被观察者
 */
public class RetryObservable {

  /**
   * 观察者
   */
  private final Set<RetrySubscribe> retrySubscribes = new HashSet<>();

  public synchronized void addRetrySubscribe(RetrySubscribe retryObserver) {
    retrySubscribes.add(retryObserver);
  }

  public synchronized void notifyAll(Attempt<?> attempt) {
    for (RetrySubscribe retryObserver : retrySubscribes) {
      retryObserver.onRetry(attempt);
    }
  }
}
