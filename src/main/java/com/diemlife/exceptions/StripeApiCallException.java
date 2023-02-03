package com.diemlife.exceptions;

import com.stripe.exception.StripeException;

public class StripeApiCallException extends RuntimeException {

    private final boolean recoverable;

    public StripeApiCallException(final String message, final StripeException cause, final boolean recoverable) {
        super(message, cause);
        this.recoverable = recoverable;
    }

    public StripeApiCallException(final String message, final StripeException cause) {
        this(message, cause, false);
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    @Override
    public synchronized StripeException getCause() {
        return (StripeException) super.getCause();
    }

}
