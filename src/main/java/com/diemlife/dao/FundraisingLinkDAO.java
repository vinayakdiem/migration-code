package com.diemlife.dao;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.exceptions.RequiredParameterMissingException;
import com.diemlife.models.BrandConfig;
import com.diemlife.models.FundraisingLink;
import com.diemlife.models.FundraisingTransaction;
import com.diemlife.models.QuestSEO;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import com.diemlife.models.UserSEO;


@Repository
public class FundraisingLinkDAO extends TypedSingletonDAO<FundraisingLink> implements FundraisingLinkRepository {

	@PersistenceContext
	EntityManager em;

    public FundraisingLink startFundraisingForQuest(final Quests quest,
                                                    final User doer,
                                                    final BrandConfig brand,
                                                    final BrandConfig secondaryBrand,
                                                    final Long targetAmountCents,
                                                    final String targetAmountCurrency,
                                                    final String campaignName,
                                                    final String coverImageUrl) {
        checkRequiredParameters(quest, doer);
        if (targetAmountCents == null) {
            throw new RequiredParameterMissingException("targetAmountCents");
        }
        if (targetAmountCurrency == null) {
            throw new RequiredParameterMissingException("targetAmountCurrency");
        }
        final FundraisingLink fundraisingLink = new FundraisingLink();
        fundraisingLink.fundraiser = doer;
        fundraisingLink.quest = quest;
        fundraisingLink.brand = brand;
        fundraisingLink.secondaryBrand = secondaryBrand;
        fundraisingLink.active = true;
        fundraisingLink.displayBtn = true;
        fundraisingLink.createdOn = Calendar.getInstance().getTime();
        fundraisingLink.targetAmountCents = targetAmountCents;
        fundraisingLink.targetAmountCurrency = targetAmountCurrency;
        fundraisingLink.campaignName = trimToNull(campaignName);
        fundraisingLink.coverImageUrl = trimToNull(coverImageUrl);
        fundraisingLink.transactions = new ArrayList<>();
        return save(fundraisingLink, FundraisingLink.class);
    }

    public FundraisingLink updateFundraisingForQuest(final FundraisingLink fundraisingLink,
                                                     final Long targetAmountCents,
                                                     final String campaignName,
                                                     final String coverImageUrl) {
        fundraisingLink.targetAmountCents = targetAmountCents;
        fundraisingLink.campaignName = trimToNull(campaignName);
        fundraisingLink.coverImageUrl = trimToNull(coverImageUrl);
        return save(fundraisingLink, FundraisingLink.class);
    }

    public FundraisingLink stopFundraisingForQuest(final Quests quest, final User doer) {
        checkRequiredParameters(quest, doer);
        final FundraisingLink fundraisingLink = getFundraisingLinkForQuestAndDoer(quest.getId(), doer.getId());
        if (fundraisingLink == null) {
            return null;
        } else {
            fundraisingLink.active = false;
            return save(fundraisingLink, FundraisingLink.class);
        }
    }

    public FundraisingLink getFundraisingLink(final Quests quest, final User doer) {
        checkRequiredParameters(quest, doer);
        return getFundraisingLinkForQuestAndDoer(quest.getId(), doer.getId());
    }

    public List<FundraisingLink> getQuestFundraisingLinks(final QuestSEO quest) {
        if (quest == null) {
            throw new RequiredParameterMissingException("quest");
        }
        return em.createQuery(
                "SELECT fl " +
                        "FROM FundraisingLinks fl " +
                        "WHERE fl.quest.id = :questId " +
                        "AND fl.active = true",
                FundraisingLink.class)
                .setParameter("questId", quest.getId())
                .getResultList();
    }

    public List<FundraisingLink> getUserFundraisingLinks(final UserSEO user) {
        if (user == null) {
            throw new RequiredParameterMissingException("user");
        }
        return em.createQuery(
                "SELECT fl " +
                        "FROM FundraisingLinks fl " +
                        "WHERE fl.fundraiser.id = :userId " +
                        "AND fl.active = true",
                FundraisingLink.class)
                .setParameter("userId", user.getId())
                .getResultList();
    }

    public FundraisingLink addBackingTransaction(final FundraisingLink fundraisingLink,
                                                 final FundraisingTransaction transaction) {
        if (fundraisingLink == null) {
            throw new RequiredParameterMissingException("fundraisingLink");
        }
        if (transaction == null) {
            throw new RequiredParameterMissingException("transaction");
        }
        fundraisingLink.transactions.add(transaction);
        return save(fundraisingLink, FundraisingLink.class);
    }

    public boolean existsWithCampaignName(final String campaignName) {
        return (em
                .createQuery("SELECT COUNT(fl) FROM FundraisingLinks fl WHERE LOWER(fl.campaignName) LIKE :campaignName", Long.class)
                .setParameter("campaignName", (campaignName).toLowerCase())
                .getSingleResult()) > 0;
    }

    public boolean existsWithQuestAndFundraiserUser(final QuestSEO quest, final UserSEO fundraiser) {
        checkRequiredParameters(quest, fundraiser);
        return em
                .createQuery("" +
                        "SELECT COUNT(fl) " +
                        "FROM FundraisingLinks fl " +
                        "WHERE fl.quest.id = :questId " +
                        "AND fl.fundraiser.id = :fundraiserId", Long.class)
                .setParameter("questId", quest.getId())
                .setParameter("fundraiserId", fundraiser.getId())
                .getSingleResult() > 0;
    }

    private FundraisingLink getFundraisingLinkForQuestAndDoer(final Integer questId, final Integer doerId) {
        if (questId == null) {
            throw new RequiredParameterMissingException("questId");
        }
        if (doerId == null) {
            throw new RequiredParameterMissingException("doerId");
        }
            
                return em.createQuery(
                        "SELECT fl " +
                                "FROM FundraisingLinks fl " +
                                "WHERE fl.quest.id = :questId " +
                                "AND fl.fundraiser.id = :userId " +
                                "AND fl.active = true",
                        FundraisingLink.class)
                        .setParameter("questId", questId)
                        .setParameter("userId", doerId)
                        .getSingleResult();
    }

    private void checkRequiredParameters(final QuestSEO quest, final UserSEO doer) {
        if (quest == null) {
            throw new RequiredParameterMissingException("quest");
        }
        if (doer == null) {
            throw new RequiredParameterMissingException("doer");
        }
    }

}
