package retry.caller;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 固定超时时间
 */
public class FixedTimeAttemptCaller<V> implements AttemptCaller<V> {

  private final long timeout;
  private final TimeUnit timeUnit;
  private final ExecutorService executorService;

  public FixedTimeAttemptCaller(ExecutorService executorService,
      long timeout, TimeUnit timeUnit) {
    this.timeout = timeout;
    this.timeUnit = timeUnit;
    this.executorService = executorService;
  }

  @Override
  public V call(Callable<V> callable)
      throws InterruptedException, TimeoutException, ExecutionException {
    final Future<V> future = executorService.submit(callable);
    try {
      // 超时同步获取结果
      return future.get(timeout, timeUnit);
    } catch (InterruptedException | TimeoutException e) {
      future.cancel(true);
      throw e;
    }
  }
}
