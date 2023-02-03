package com.diemlife.exceptions;

import static java.lang.String.format;

public class RequiredParameterMissingException extends IllegalArgumentException {

    private final String parameterName;

    public RequiredParameterMissingException(final String parameterName) {
        this.parameterName = parameterName;
    }

    @Override
    public String getLocalizedMessage() {
        return format("Required parameter missing: '%s'", parameterName);
    }

}
