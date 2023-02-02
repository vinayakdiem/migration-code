package com.diemlife.acl;

import com.diemlife.models.Quests;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class QuestsListWithACL extends ListWithACL<Quests> {

    public QuestsListWithACL(final Supplier<List<Quests>> questsSupplier, final EntityManager entityManager) {
        super(questsSupplier, candidate -> new QuestACL(candidate, entityManager));
    }

    public static QuestsListWithACL emptyListWithACL() {
        return new QuestsListWithACL(Collections::emptyList, null);
    }

}
