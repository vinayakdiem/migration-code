package com.diemlife.utils;

import static com.google.common.collect.Iterables.getLast;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.diemlife.constants.Util;
import com.diemlife.dto.CollectionRequestDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.HasId;
import com.stripe.model.StripeCollection;

import play.Logger;

*//**
 * Created by andrewcoleman on 1/22/17.
 *//*
public class StripeUtils {

    public static <T extends HasId, CR extends CollectionRequestDTO> List<T> loadEntireCollection(final Function<CR, StripeCollection<T>> loader,
                                                                                                  final Supplier<CR> requestFactory,
                                                                                                  final int pageSize) {
        return new StripeCollectionLoader<>(loader, requestFactory, pageSize).loadEntireCollection();
    }

    private static class StripeCollectionLoader<T extends HasId, CR extends CollectionRequestDTO> {
        private final Function<CR, StripeCollection<T>> loader;
        private final Supplier<CR> requestFactory;
        private final int pageSize;

        private StripeCollectionLoader(final Function<CR, StripeCollection<T>> loader,
                                       final Supplier<CR> requestFactory,
                                       final int pageSize) {
            this.loader = loader;
            this.requestFactory = requestFactory;
            this.pageSize = pageSize;
        }

        private List<T> loadEntireCollection() {
            final List<T> result = new ArrayList<>();
            final CR request = requestFactory.get();
            request.limit = pageSize;
            populateResults(result, request);
            return result;
        }

        private void populateResults(final List<T> result,
                                     final CR request) {
            final StripeCollection<T> collection = loader.apply(request);
            if (collection == null) {
                return;
            }
            final List<T> data = collection.getData();
            if (!Util.isEmpty(data)) {
                result.addAll(data);
            }
            if (isTrue(collection.getHasMore())) {
                final T last = getLast(result);
                request.startingAfter = last.getId();
                populateResults(result, request);
            }
        }
    }

    public static void logStripeError(final String message, final StripeException e) {
        Logger.error(format("/!\\ Stripe call failed\n" +
                        "---- Request ID : %s\n" +
                        "---- Response code : %s\n" +
                        "---- Original message : %s\n" +
                        "---- Message : %s\n",
                e.getRequestId(),
                e.getStatusCode(),
                e.getLocalizedMessage(),
                message
        ), e);
    }

}
