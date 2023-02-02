package com.diemlife.acl;

import com.diemlife.models.User;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.diemlife.acl.VoterPredicate.VotingResult.For;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

public abstract class ListWithACL<T> extends WithACL<T> {

    private final Supplier<List<T>> listSupplier;

    ListWithACL(final Supplier<List<T>> listSupplier,
                final Function<User, ACL<T>> aclFactory) {
        super(aclFactory);
        this.listSupplier = listSupplier;
    }

    public List<T> getList(final User candidate) {
        final List<T> list = listSupplier.get();
        return isEmpty(list) ? emptyList() : list.stream()
                .filter(element -> For.equals(aclFactory.apply(candidate).test(element)))
                .collect(toList());
    }

    public long getCount(final User candidate) {
        final List<T> list = listSupplier.get();
        return isEmpty(list) ? 0L : list.stream()
                .filter(element -> For.equals(aclFactory.apply(candidate).test(element)))
                .count();
    }

}
