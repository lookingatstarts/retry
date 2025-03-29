package retry.caller;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface AttemptCaller<V> {

  /**
   * 响应中断
   */
  V call(Callable<V> callable) throws ExecutionException, TimeoutException, InterruptedException;
}
