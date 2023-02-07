package com.diemlife.services;

import com.typesafe.config.Config;
import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.dao.FundraisingLinkDAO;
import com.diemlife.dao.FundraisingLinkRepository;
import com.diemlife.dto.BrandConfigDTO;
import com.diemlife.dto.FundraisingLinkDTO;
import com.diemlife.dto.QuestDTO;
import com.diemlife.dto.UserDTO;
import com.diemlife.models.BrandConfig;
import com.diemlife.models.FundraisingLink;
import com.diemlife.models.FundraisingTransaction;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static com.diemlife.utils.URLUtils.publicQuestSEOSlugs;

@Service
public class FundraisingService implements FundraisingLinkRepository {

	@Autowired
    private Config configuration;
	
	@Autowired
    private FundraisingLinkDAO fundraisingLinkDAO;

//    @Transactional
    @Override
    public FundraisingLink getFundraisingLink(final Quests quest, final User doer) {
        return fundraisingLinkDAO.getFundraisingLink(quest, doer);
    }

//    @Transactional
    @Override
    public FundraisingLink addBackingTransaction(final FundraisingLink fundraisingLink,
                                                 final FundraisingTransaction transaction) {
        return fundraisingLinkDAO.addBackingTransaction(fundraisingLink, transaction);
    }

//    @Transactional
    public FundraisingLinkDTO getFundraisingLinkDTO(final @NotNull Integer questId, final @NotNull Integer doerId) {
        return convert(getFundraisingLinkModel(questId, doerId));
    }

//    @Transactional
    public List<FundraisingLinkDTO> getQuestFundraisingLinks(final @NotNull Integer questId) {
//FIXME Vinayak
    	return null;
//        return fundraisingLinkDAO.getQuestFundraisingLinks(em.find(Quests.class, questId))
//                .stream()
//                .map(this::convert)
//                .collect(toList());
    }

//    @Transactional
    public List<FundraisingLinkDTO> getUserFundraisingLinks(final @NotNull Integer userId) {
    	//FIXME Vinayak
    	return null;
//        final User user = em.find(User.class, userId);
//        return fundraisingLinkDAO.getUserFundraisingLinks(user)
//                .stream()
//                .map(this::convert)
//                .collect(toList());
    }

    // TODO: How do we bootstrap fundraising detail for a parent quest?  Is this approach correct?
    public FundraisingLinkDTO getFundraisingLinkForParentQuest(int questId, int userId) {
    	//FIXME Vinayak
//        final Quests quest = em.find(Quests.class, questId);
//        final User doer = em.find(User.class, userId);

        FundraisingLinkDTO dto = new FundraisingLinkDTO();
      //FIXME Vinayak
//        dto.creator =  UserDTO.toDTO(quest.getUser());

        // TODO: fill in more stuff
        final String envUrl = configuration.getString(DeploymentEnvironments.valueOf(configuration.getString("application.mode")).getBaseUrlKey());
        dto.id = null;
        dto.brand = /* BrandConfigDTO.toDto(link.brand) */ null;
        dto.secondaryBrand = /* BrandConfigDTO.toDto(link.secondaryBrand) */ null;
      //FIXME Vinayak
//        dto.doer = UserDTO.toDTO(doer);
        dto.campaignName = /* link.campaignName */ null;
        dto.coverImageUrl = /* link.coverImageUrl */ null;
        dto.quest = /* QuestDTO.toDTO(quest).withSEOSlugs(publicQuestSEOSlugs(quest, quest.getUser(), envUrl)) */ null;
        dto.currentAmount = 0L;
        dto.timesBacked = 0;
        dto.targetAmount = /* link.targetAmountCents == null ? null : link.targetAmountCents.intValue() */ null;
        dto.currency = /* link.targetAmountCurrency */ "usd";
        dto.displayBtn = /* link.displayBtn */ false;
        dto.active = /* link.active */ true;
        dto.endDate = /* link.endDate */ null;

        return dto;
    }

    // TODO: can we steal attributes from a child with a fundraising link to populate a parent?
    public void patchFundraisingLinkForParentQuestFromChild(FundraisingLinkDTO childDto, FundraisingLinkDTO parentDto) {
        // Update the parent...
    }

    private FundraisingLink getFundraisingLinkModel(final Integer questId, final Integer doerId) {
    	//FIXME Vinayak
//        final User doer = em.find(User.class, doerId);
//        final Quests quest = em.find(Quests.class, questId);
//        if (doer == null || quest == null) {
            return null;
//        } else {
//            return fundraisingLinkDAO.getFundraisingLink(quest, doer);
//        }
    }

//    @Transactional(readOnly = true)
    public boolean fundraisingLinkExists(final @NotNull Quests quest, final @NotNull User fundraiser) {
        return quest.isFundraising() && fundraisingLinkDAO.existsWithQuestAndFundraiserUser(quest, fundraiser);
    }

//    @Transactional
    public FundraisingLinkDTO startFundraising(final @NotNull Quests quest,
                                               final @NotNull User fundraiser,
                                               final @NotNull Long targetAmountInCents,
                                               final @NotNull String targetAmountCurrency,
                                               final BrandConfig brand,
                                               final BrandConfig secondaryBrand,
                                               final String campaignName,
                                               final String coverImageUrl) {
        Logger.info(format("Starting fundraising by fundraiser user [%s] and Quest [%s]", fundraiser.getId(), quest.getId()));

        if (!quest.isFundraising()) {
            Logger.warn(format("Cannot start fundraiser by user [%s] - Quest [%s] is not configured for fundraising", fundraiser.getId(), quest.getId()));

            return null;
        }
        final FundraisingLink existing = fundraisingLinkDAO.getFundraisingLink(quest, fundraiser);
        if (existing == null) {
            final String currency = isBlank(targetAmountCurrency)
                    ? configuration.getString("application.currency")
                    : targetAmountCurrency;
            final FundraisingLink link = fundraisingLinkDAO.startFundraisingForQuest(quest, fundraiser, brand, secondaryBrand, targetAmountInCents, currency, campaignName, coverImageUrl);

            Logger.info(format("New fundraising link [%s] created", link.id));

            return convert(link, fundraiser);
        } else {
            Logger.info(format("Fundraising already started by fundraiser user [%s] and Quest [%s] - updating", fundraiser.getId(), quest.getId()));

            final FundraisingLink updated = fundraisingLinkDAO.updateFundraisingForQuest(existing, targetAmountInCents, campaignName, coverImageUrl);

            Logger.info(format("Exiting fundraising link [%s] updated", updated.id));

            return convert(updated, fundraiser);
        }
    }

    private FundraisingLinkDTO convert(final FundraisingLink link) {
        return convert(link, null);
    }

    private FundraisingLinkDTO convert(final FundraisingLink link, final User creator) {
        if (link == null) {
            return null;
        }
        final FundraisingLinkDTO result = new FundraisingLinkDTO();
        if (creator != null) {
            result.creator = UserDTO.toDTO(creator);
        } else {
            result.creator = UserDTO.toDTO(link.quest.getUser());
        }

        final String envUrl = configuration.getString(DeploymentEnvironments.valueOf(configuration.getString("application.mode")).getBaseUrlKey());
        result.id = link.id;
        result.doer = UserDTO.toDTO(link.fundraiser);
        result.brand = BrandConfigDTO.toDto(link.brand);
        result.secondaryBrand = BrandConfigDTO.toDto(link.secondaryBrand);
        result.quest = QuestDTO.toDTO(link.quest).withSEOSlugs(publicQuestSEOSlugs(link.quest, link.quest.getUser(), envUrl));
        result.campaignName = link.campaignName;
        result.coverImageUrl = link.coverImageUrl;
        result.targetAmount = link.targetAmountCents == null ? null : link.targetAmountCents.intValue();
        result.currentAmount = 0L;
        result.currency = link.targetAmountCurrency;
        result.timesBacked = 0;
        result.displayBtn = link.displayBtn;
        result.active = link.active;
        result.endDate = link.endDate;

        return result;
    }

//    @Transactional
    public FundraisingLinkDTO setupParties(final @NotNull Integer questId, final @NotNull Integer doerId) {
    	//FIXME Vinayak
//        final Quests quest = em.find(Quests.class, questId);
        final FundraisingLinkDTO result = new FundraisingLinkDTO();
      //FIXME Vinayak
//        if (quest != null && quest.isFundraising()) {
//            final String envUrl = configuration.getString(DeploymentEnvironments.valueOf(configuration.getString("application.mode")).getBaseUrlKey());
//            result.doer = UserDTO.toDTO(em.find(User.class, doerId));
//            result.creator = UserDTO.toDTO(em.find(User.class, quest.getCreatedBy()));
//            result.quest = QuestDTO.toDTO(quest).withSEOSlugs(publicQuestSEOSlugs(quest, quest.getUser(), envUrl));
//        }
        return result;
    }

}
