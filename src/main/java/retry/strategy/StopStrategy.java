package retry.strategy;

import retry.attempt.Attempt;

/**
 * 通知重试策略
 */
public interface StopStrategy {

  /**
   * 是否还进行重试
   */
  boolean shouldStop(Attempt<?> failedAttempt);
}
