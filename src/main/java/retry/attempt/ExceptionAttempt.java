package retry.attempt;

import java.util.concurrent.ExecutionException;

public final class ExceptionAttempt<R> implements Attempt<R> {
    private final ExecutionException exception;
    private final long attemptTimes;
    private final long delaySinceFirstAttempt;

    public ExceptionAttempt(Throwable cause, long attemptTimes, long delaySinceFirstAttempt) {
        this.exception = new ExecutionException(cause);
        this.attemptTimes = attemptTimes;
        this.delaySinceFirstAttempt = delaySinceFirstAttempt;
    }

    @Override
    public boolean hasResult() {
        return false;
    }

    @Override
    public boolean hasException() {
        return true;
    }

    @Override
    public R get() throws ExecutionException {
        throw this.exception;
    }

    @Override
    public R getResult() throws IllegalStateException {
        throw new IllegalStateException("this attempt has exception");
    }

    @Override
    public Throwable getCause() throws IllegalStateException {
        return this.exception.getCause();
    }

    @Override
    public long getAttemptTimes() {
        return this.attemptTimes;
    }

    @Override
    public long getDelaySinceFirstAttempt() {
        return this.delaySinceFirstAttempt;
    }
}
