package retry.caller;

import java.util.concurrent.*;

public class FixedTimeAttemptCaller<V> implements AttemptCaller<V> {

    private long timeout;
    private TimeUnit timeUnit;
    private ExecutorService executorService;

    public FixedTimeAttemptCaller(ExecutorService executorService, long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.executorService = executorService;
    }

    @Override
    public V call(Callable<V> callable) throws InterruptedException, TimeoutException, ExecutionException {
        final Future<V> future = executorService.submit(callable);
        try {
            return future.get(timeout, timeUnit);
        } catch (InterruptedException | TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }
}
