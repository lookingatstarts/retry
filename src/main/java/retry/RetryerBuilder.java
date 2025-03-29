/*
 * Copyright 2012-2015 Ray Holder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package retry;

import java.util.function.Predicate;
import retry.attempt.Attempt;
import retry.caller.AttemptCaller;
import retry.publish.RetryObservable;
import retry.strategy.BlockStrategies;
import retry.strategy.BlockStrategy;
import retry.strategy.StopStrategies;
import retry.strategy.StopStrategy;
import retry.strategy.WaitStrategies;
import retry.strategy.WaitStrategy;

/**
 * 建造者模式
 */
public class RetryerBuilder<V> {

  protected RetryObservable retryObservable;
  private AttemptCaller<V> attemptCaller;
  private StopStrategy stopStrategy;
  private WaitStrategy waitStrategy;
  private BlockStrategy blockStrategy;
  private Predicate<Attempt<V>> attemptPredicate = attempt -> false;

  public static <V> RetryerBuilder<V> newBuilder() {
    return new RetryerBuilder<V>();
  }

  public RetryerBuilder<V> caller(AttemptCaller<V> attemptCaller) {
    this.attemptCaller = attemptCaller;
    return this;
  }

  public RetryerBuilder<V> retryObservable(RetryObservable retryObservable) {
    if (retryObservable == null) {
      throw new IllegalArgumentException("retryObservable cannot be null");
    }
    this.retryObservable = retryObservable;
    return this;
  }

  public RetryerBuilder<V> withWaitStrategy(WaitStrategy waitStrategy)
      throws IllegalStateException {
    if (waitStrategy == null) {
      throw new IllegalArgumentException("waitStrategy must not be null");
    }
    if (this.waitStrategy != null) {
      throw new IllegalStateException("waitStrategy already set");
    }
    this.waitStrategy = waitStrategy;
    return this;
  }

  public RetryerBuilder<V> withStopStrategy(StopStrategy stopStrategy)
      throws IllegalStateException {
    if (stopStrategy == null) {
      throw new IllegalArgumentException("stopStrategy must not be null");
    }
    if (this.stopStrategy != null) {
      throw new IllegalStateException("stopStrategy already set");
    }
    this.stopStrategy = stopStrategy;
    return this;
  }


  public RetryerBuilder<V> withBlockStrategy(BlockStrategy blockStrategy)
      throws IllegalStateException {
    if (blockStrategy == null) {
      throw new IllegalArgumentException("blockStrategy must not be null");
    }
    if (this.blockStrategy != null) {
      throw new IllegalStateException("blockStrategy already set");
    }
    this.blockStrategy = blockStrategy;
    return this;
  }


  public RetryerBuilder<V> retryIfException() {
    attemptPredicate = attemptPredicate.or(new ExceptionClassPredicate<V>(Exception.class));
    return this;
  }

  public RetryerBuilder<V> retryIfRuntimeException() {
    attemptPredicate = attemptPredicate.or(
        new ExceptionClassPredicate<V>(RuntimeException.class));
    return this;
  }

  public RetryerBuilder<V> retryIfExceptionOfType(Class<? extends Throwable> exceptionClass) {
    if (exceptionClass == null) {
      throw new IllegalArgumentException("exceptionClass must not be null");
    }
    attemptPredicate = attemptPredicate.or(new ExceptionClassPredicate<>(exceptionClass));
    return this;
  }

  public RetryerBuilder<V> retryIfException(Predicate<Throwable> exceptionPredicate) {
    if (exceptionPredicate == null) {
      throw new IllegalArgumentException("exceptionPredicate must not be null");
    }
    attemptPredicate = attemptPredicate.or(new ExceptionPredicate<V>(exceptionPredicate));
    return this;
  }

  public RetryerBuilder<V> retryIfResult(Predicate<V> resultPredicate) {
    if (resultPredicate == null) {
      throw new IllegalArgumentException("resultPredicate must not be null");
    }
    attemptPredicate = attemptPredicate.or(new ResultPredicate<V>(resultPredicate));
    return this;
  }

  public Retryer<V> build() {
    StopStrategy theStopStrategy = stopStrategy == null ? StopStrategies.neverStop() : stopStrategy;
    WaitStrategy theWaitStrategy = waitStrategy == null ? WaitStrategies.noWait() : waitStrategy;
    BlockStrategy theBlockStrategy =
        blockStrategy == null ? BlockStrategies.threadSleepStrategy() : blockStrategy;
    return new Retryer<>(attemptCaller, retryObservable, attemptPredicate,
        theStopStrategy, theWaitStrategy, theBlockStrategy);
  }

  private static final class ExceptionClassPredicate<V> implements Predicate<Attempt<V>> {

    private final Class<? extends Throwable> exceptionClass;

    public ExceptionClassPredicate(Class<? extends Throwable> exceptionClass) {
      this.exceptionClass = exceptionClass;
    }

    @Override
    public boolean test(Attempt<V> attempt) {
      if (!attempt.hasException()) {
        return false;
      }
      return exceptionClass.isAssignableFrom(attempt.getCause().getClass());
    }
  }

  private static final class ResultPredicate<V> implements Predicate<Attempt<V>> {

    private final Predicate<V> delegate;

    public ResultPredicate(Predicate<V> delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean test(Attempt<V> attempt) {
      if (!attempt.hasResult()) {
        return false;
      }
      V result = attempt.getResult();
      return delegate.test(result);
    }
  }

  private static final class ExceptionPredicate<V> implements Predicate<Attempt<V>> {

    private final Predicate<Throwable> delegate;

    public ExceptionPredicate(Predicate<Throwable> delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean test(Attempt<V> attempt) {
      if (!attempt.hasException()) {
        return false;
      }
      return delegate.test(attempt.getCause());
    }
  }
}
