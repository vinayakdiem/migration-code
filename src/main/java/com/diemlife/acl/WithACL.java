package com.diemlife.acl;

import com.diemlife.models.User;

import java.util.function.Function;

abstract class WithACL<T> {

    protected final Function<User, ACL<T>> aclFactory;

    protected WithACL(final Function<User, ACL<T>> aclFactory) {
        this.aclFactory = aclFactory;
    }

}
