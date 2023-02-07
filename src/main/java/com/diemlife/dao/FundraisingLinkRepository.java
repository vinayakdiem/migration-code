package com.diemlife.dao;

import org.springframework.stereotype.Repository;

import com.diemlife.models.FundraisingLink;
import com.diemlife.models.FundraisingTransaction;
import com.diemlife.models.Quests;
import com.diemlife.models.User;

@Repository
public interface FundraisingLinkRepository {

    FundraisingLink getFundraisingLink(final Quests quest, final User doer);

    FundraisingLink addBackingTransaction(final FundraisingLink fundraisingLink, final FundraisingTransaction transaction);

}
