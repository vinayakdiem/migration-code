package com.diemlife.acl;

import com.diemlife.models.Quests;

import javax.persistence.EntityManager;
import java.util.function.Supplier;

public class QuestEntityWithACL extends EntityWithACL<Quests> {

    public QuestEntityWithACL(final Supplier<Quests> entitySupplier, final EntityManager entityManager) {
        super(entitySupplier, candidate -> new QuestACL(candidate, entityManager));
    }

}
