package com.diemlife.acl;

public abstract class Lazy<T> {

    private boolean untouched = true;
    private T instance = null;

    protected abstract T init();

    public T get() {
        if (untouched) {
            try {
                instance = init();
            } finally {
                untouched = false;
            }
        }
        return instance;
    }

}
