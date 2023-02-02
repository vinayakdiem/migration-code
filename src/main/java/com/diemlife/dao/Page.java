package com.diemlife.dao;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class Page<T> extends AbstractPage<List<T>> {

    private final List<T> data = new ArrayList<>();

    public Page(final int start, final int limit, final boolean more) {
        super(start, limit, more);
    }

    public Page<T> withData(final List<T> data) {
        if (isNotEmpty(data)) {
            this.data.addAll(data);
        }
        return this;
    }

    public List<T> getData() {
        return data;
    }

}
