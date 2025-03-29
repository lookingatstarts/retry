package retry.caller;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * 不超时获取结果
 */
public class NoTimeLimitedAttemptCaller<V> implements AttemptCaller<V> {

  private final ExecutorService executorService;

  public NoTimeLimitedAttemptCaller(ExecutorService executorService) {
    this.executorService = executorService;
  }

  @Override
  public V call(Callable<V> callable) throws InterruptedException, ExecutionException {
    return executorService.submit(callable).get();
  }
}
