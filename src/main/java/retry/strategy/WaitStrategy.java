package retry.strategy;


import retry.attempt.Attempt;

public interface WaitStrategy {

    /**
     * 下一次执行等待时间(毫秒)
     */
    long nextSleepTime(Attempt<?> failedAttempt);
}
