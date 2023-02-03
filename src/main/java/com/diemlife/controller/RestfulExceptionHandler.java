package com.diemlife.controller;

import com.typesafe.config.Config;
import com.diemlife.dto.SystemFailureDTO;
import com.diemlife.exceptions.StripeApiCallException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import play.Environment;
import play.Logger;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.libs.Json;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import com.diemlife.services.OutgoingEmailService;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.mvc.Results.internalServerError;

public class RestfulExceptionHandler extends DefaultHttpErrorHandler {

    private final OutgoingEmailService emailService;

    @Inject
    public RestfulExceptionHandler(final Config configuration,
                                   final Environment environment,
                                   final OptionalSourceMapper sourceMapper,
                                   final Provider<Router> routes,
                                   final OutgoingEmailService emailService) {
        super(configuration, environment, sourceMapper, routes);
        this.emailService = emailService;
    }

    @Override
    protected CompletionStage<Result> onDevServerError(final RequestHeader request, final UsefulException exception) {
        try {
            Logger.error(exception.toString(), exception);

            return completedFuture(internalServerError(Json.toJson(fromUsefulExceptionWithLogging(exception, true, true))));
        } catch (final Exception e) {
            Logger.error(e.getMessage(), e);

            return completedFuture(internalServerError());
        }
    }

    @Override
    protected CompletionStage<Result> onProdServerError(final RequestHeader request, final UsefulException exception) {
        try {
            Logger.error(exception.toString(), exception);

            final SystemFailureDTO fullReport = fromUsefulExceptionWithLogging(exception, true, true);

            emailService.sendProductionFailureEmail(request, fullReport);

            return completedFuture(internalServerError(Json.toJson(fromUsefulExceptionWithLogging(exception, false, false))));
        } catch (final Exception e) {
            Logger.error(e.getMessage(), e);

            return completedFuture(internalServerError());
        }
    }

    private SystemFailureDTO fromUsefulExceptionWithLogging(final UsefulException exception,
                                                            final boolean withMessage,
                                                            final boolean withStackTrace) {
        final SystemFailureDTO result = new SystemFailureDTO();
        result.id = exception.id;
        result.title = exception.title;
        result.description = exception.description;

        ExceptionUtils.getThrowableList(exception).forEach(cause -> {
            if (cause instanceof StripeApiCallException && StripeApiCallException.class.cast(cause).isRecoverable()) {
                result.recoverable = true;
            }
        });

        if (withMessage) {
            result.message = ExceptionUtils.getRootCause(exception).getMessage();
        }
        if (withStackTrace) {
            result.stack = getStackTrace(exception);
        }
        return result;
    }

    private String getStackTrace(final UsefulException exception) {
        try (final StringWriter sw = new StringWriter(); final PrintWriter pw = new PrintWriter(sw)) {
            exception.printStackTrace(pw);
            return sw.getBuffer().toString();
        } catch (final IOException e) {
            Logger.error(e.getMessage(), e);
            return null;
        }
    }

}
