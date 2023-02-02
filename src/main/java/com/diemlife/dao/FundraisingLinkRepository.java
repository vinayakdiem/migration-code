package com.diemlife.dao;

import models.FundraisingLink;
import models.FundraisingTransaction;
import models.Quests;
import models.User;

public interface FundraisingLinkRepository {

    FundraisingLink getFundraisingLink(final Quests quest, final User doer);

    FundraisingLink addBackingTransaction(final FundraisingLink fundraisingLink, final FundraisingTransaction transaction);

}
