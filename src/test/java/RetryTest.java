import org.junit.Test;
import retry.Retry;
import retry.RetryBuilder;
import retry.attempt.Attempt;
import retry.caller.FixedTimeAttemptCaller;
import retry.strategy.StopStrategies;
import retry.strategy.WaitStrategies;
import retry.subscribe.RetrySubscribe;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryTest {

    @Test
    public void test() {
        final AtomicInteger acc = new AtomicInteger(0);
        ExecutorService executorService = new ThreadPoolExecutor(2, 2, 10L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), r -> {
            final Thread thread = new Thread(r);
            final int i = acc.get();
            if (i > 1000) {
                acc.set(0);
            }
            thread.setName("retry-worker" + acc.getAndIncrement());
            return thread;
        });
        final Retry<Long> retry = RetryBuilder.<Long>newBuilder()
                // 每次执行设置超时时间
                .caller(new FixedTimeAttemptCaller<>(executorService, 2L, TimeUnit.SECONDS))
                .retryIfResult(result -> result < 10L)
                .retryIfExceptionOfType(IllegalArgumentException.class)
                .retrySubscribe(new RetrySubscribe() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        System.out.println("第" + attempt.getAttemptTimes() + "次尝试");
                    }
                })
                // 结束条件
                .stopStrategy(StopStrategies.stopAfterAttempt(2L))
                // 执行间隔时间
                .waitStrategy(WaitStrategies.fixedWaitStrategy(4L, TimeUnit.SECONDS))
                .builder();
        try {
            System.out.println(retry.call(this::service));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public Long service() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 100L;
    }
}
