package com.diemlife.acl;

import com.diemlife.models.User;

abstract class ACL<T> implements VoterPredicate<T> {

    final User candidate;

    ACL(final User candidate) {
        this.candidate = candidate;
    }

}
