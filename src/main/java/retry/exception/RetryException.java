package retry.exception;

import retry.attempt.Attempt;

public final class RetryException extends RuntimeException {

    private static final long serialVersionUID = 1702864786683822063L;

    private final Attempt<?> lastFailedAttempt;

    public RetryException(Attempt<?> lastFailedAttempt) {
        this("Retrying failed to complete successfully after " + lastFailedAttempt.getAttemptTimes() + " attempts.", lastFailedAttempt);
    }

    public RetryException(String message, Attempt<?> lastFailedAttempt) {
        super(message, lastFailedAttempt.getCause());
        this.lastFailedAttempt = lastFailedAttempt;
    }

    public Attempt<?> getLastFailedAttempt() {
        return lastFailedAttempt;
    }
}
