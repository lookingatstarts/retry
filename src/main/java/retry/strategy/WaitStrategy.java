package retry.strategy;


import retry.attempt.Attempt;

public interface WaitStrategy {

  /**
   * 阻塞时间ms
   */
  long computeSleepTime(Attempt<?> failedAttempt);
}
