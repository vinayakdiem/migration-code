package com.diemlife.exceptions;

import com.stripe.exception.StripeException;

public class StripeInvalidPostalCodeException extends StripeApiCallException {

    public StripeInvalidPostalCodeException(final String message, final StripeException cause) {
        super(message, cause, true);
    }

}
