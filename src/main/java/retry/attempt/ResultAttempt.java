package retry.attempt;

import java.util.concurrent.ExecutionException;

public final class ResultAttempt<R> implements Attempt<R> {

    private final R result;
    private final long attemptTimes;
    private final long delaySinceFirstAttempt;

    public ResultAttempt(R result, long attemptTimes, long delaySinceFirstAttempt) {
        this.result = result;
        this.attemptTimes = attemptTimes;
        this.delaySinceFirstAttempt = delaySinceFirstAttempt;
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public boolean hasException() {
        return false;
    }

    @Override
    public R get() throws ExecutionException {
        return this.result;
    }

    @Override
    public R getResult() throws IllegalStateException {
        return this.result;
    }

    @Override
    public Throwable getCause() throws IllegalStateException {
        throw new IllegalStateException("this attempt has result");
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
