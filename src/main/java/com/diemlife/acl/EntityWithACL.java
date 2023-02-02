package com.diemlife.acl;

import com.diemlife.acl.VoterPredicate.VotingResult;
import com.diemlife.models.User;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.diemlife.acl.VoterPredicate.VotingResult.For;

public abstract class EntityWithACL<T> extends WithACL<T> {

    private final Supplier<T> entitySupplier;

    EntityWithACL(final Supplier<T> entitySupplier, final Function<User, ACL<T>> aclFactory) {
        super(aclFactory);
        this.entitySupplier = entitySupplier;
    }

    public VotingResult eligible(final User candidate) {
        final T entity = entitySupplier.get();
        return aclFactory.apply(candidate).test(entity);
    }

    public T getEntity(final User candidate) {
        final T entity = entitySupplier.get();
        return For.equals(aclFactory.apply(candidate).test(entity)) ? entity : null;
    }

}
