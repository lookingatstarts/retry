package retry.strategy;


import retry.attempt.Attempt;

public interface StopStrategy {

    /**
     * @return true 停止执行，不重试了
     */
    boolean shouldStop(Attempt<?> failedAttempt);
}
