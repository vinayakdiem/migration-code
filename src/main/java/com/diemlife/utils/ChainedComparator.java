package com.diemlife.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ChainedComparator<T extends Comparable> implements Comparator<T> {

    private final List<Comparator<T>> listComparators;

    @SafeVarargs
    public ChainedComparator(final Comparator<T>... comparators) {
        this.listComparators = Arrays.asList(comparators);
    }

    @Override
    public int compare(final T left, final T right) {
        for (final Comparator<T> comparator : listComparators) {
            int result = comparator.compare(left, right);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

}
