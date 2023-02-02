package com.diemlife.dao;

public abstract class AbstractPage<T> {

    private final int start;
    private final int limit;
    private final boolean more;

    AbstractPage(final int start, final int limit, final boolean more) {
        this.start = start;
        this.limit = limit;
        this.more = more;
    }

    public abstract AbstractPage withData(final T data);

    public abstract T getData();

    public int getStart() {
        return start;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isMore() {
        return more;
    }

}
