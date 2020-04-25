package retry.attempt;

import java.util.concurrent.ExecutionException;

/**
 * 一次执行结果，执行成功返回值，执行失败抛出异常
 */
public interface Attempt<V> {

    boolean hasResult();

    boolean hasException();

    V get() throws ExecutionException;

    /**
     * @throws IllegalStateException 如果执行失败
     */
    V getResult() throws IllegalStateException;

    /**
     * @throws IllegalStateException 如果执行成功
     */
    Throwable getCause() throws IllegalStateException;

    /**
     * 第几次尝试调用
     */
    long getAttemptTimes();

    /**
     * 整个retry过程耗费的时间(毫秒)
     */
    long getDelaySinceFirstAttempt();
}
