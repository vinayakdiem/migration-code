package com.diemlife.utils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class StreamUtils {
    private StreamUtils() {
        super();
    }

    public static <T> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return object -> seen.add(keyExtractor.apply(object));
    }

}
