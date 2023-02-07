package com.diemlife.services;

import com.github.slugify.Slugify;
import com.stripe.model.Account;
import com.stripe.model.Customer;
import com.typesafe.config.Config;
import com.diemlife.constants.BrandImportAction;
import com.diemlife.dao.BrandConfigDAO;
import com.diemlife.dao.SecurityRoleHome;
import com.diemlife.dao.StripeCustomerDAO;
import com.diemlife.dto.BrandConfigImportDTO;
import com.diemlife.models.BrandConfig;
import com.diemlife.models.QuestBrandConfig;
import com.diemlife.models.Quests;
import com.diemlife.models.SecurityRole;
import com.diemlife.models.StripeAccount;
import com.diemlife.models.StripeCustomer;
import com.diemlife.models.StripeEntity;
import com.diemlife.models.User;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import play.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.diemlife.controller.Application.USER_ROLE;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static com.diemlife.models.User.USER_BRAND_FLAG_TRUE;

@Service
public class BrandConfigService {

	@Autowired
    private StripeConnectService stripeConnectService;
	
    @Autowired
    private BrandConfigDAO brandConfigDao;
    
    @Autowired
    private Config config;


    public List<QuestBrandConfig> getBrandConfigsForQuest(final Integer questId) {
    	return null;
//    	FIXME Vinayak
//        final Quests quest = Optional.ofNullable(questId).map(id -> jpaApi.em().find(Quests.class, id)).orElse(null);
//        if (quest == null) {
//            Logger.warn(format("Cannot load brand configs for missing Quest with ID %s", questId));
//
//            return emptyList();
//        } else if (!quest.isMultiSellerEnabled()) {
//            Logger.warn(format("Multi-seller feature is disabled for Quest withID %s", quest.getId()));
//
//            return emptyList();
//        } else {
//            Logger.debug(format("Loading all brand configs for Quest with ID %s", quest.getId()));
//
//            return brandConfigDao.findAllQuestBrandConfigsByQuest(quest);
//        }
    }

    public BrandConfig processBrandConfigImport(final BrandConfigImportDTO dto) {
        final BrandImportAction action = BrandImportAction.from(dto.getAction());
        switch (action) {
            case CREATE:
                return processBrandConfigCreate(dto);
            case DELETE:
                return processBrandConfigDelete(dto);
            default:
                return null;
        }
    }

    private BrandConfig processBrandConfigCreate(final BrandConfigImportDTO dto) {
        Logger.debug(format("Processing brand config creation: %s", dto));

        final BrandConfig existingBrandConfig = brandConfigDao.findBrandConfigBySiteUrl(dto.getSiteUrl());
        if (existingBrandConfig != null) {
            Logger.warn(format("Brand config already exists for user '%s' with ID %s - skipping", existingBrandConfig.getUser().getEmail(), existingBrandConfig.getUserId()));

            return existingBrandConfig;
        }

        final User user = createBrandUser(dto);
        final BrandConfig brandConfig = createBrandConfig(dto, user);
        final StripeAccount stripeAccount = createStripeEntities(dto, user, brandConfig);

        Logger.info(format("Created brand config for user '%s' with ID %s and Stripe account ID '%s'", user.getEmail(), brandConfig.getUserId(), stripeAccount.stripeAccountId));

        return brandConfig;
    }

    private BrandConfig processBrandConfigDelete(final BrandConfigImportDTO dto) {
        Logger.debug(format("Processing brand config deletion: %s", dto));

        final BrandConfig brandConfig = brandConfigDao.findBrandConfigBySiteUrl(dto.getSiteUrl());
        if (brandConfig == null) {
            Logger.warn(format("Brand config not found for site URL '%s'", dto.getSiteUrl()));
        } else if (BooleanUtils.isTrue(brandConfig.getUser().getActive())) {
            Logger.warn(format("Brand config with site URL '%s' cannot be deleted as it has an active user account", dto.getSiteUrl()));
        } else {
            final User user = brandConfig.getUser();
            final List<QuestBrandConfig> questConfigs = brandConfigDao.findAllQuestBrandConfigsByUser(user);

            Logger.info(format("Deleting %s Quest brand configs for user '%s' with ID %s", questConfigs.size(), user.getEmail(), brandConfig.getUserId()));

//            FIXME Vinayak
//            questConfigs.forEach(em::remove);

            Logger.info(format("Deleting brand config for user '%s' with ID %s", user.getEmail(), brandConfig.getUserId()));

//          FIXME Vinayak
//            em.remove(brandConfig);

            Logger.info(format("Deleting Stripe config for user '%s' with ID %s", user.getEmail(), brandConfig.getUserId()));

            deleteStripeEntities(user.getStripeEntity());

            Logger.info(format("Deleting user '%s' with ID %s", user.getEmail(), brandConfig.getUserId()));
//          FIXME Vinayak
//            em.remove(user);
        }

        return brandConfig;
    }

    private User createBrandUser(final BrandConfigImportDTO dto) {
        final User user = new User();

        user.setFirstName(dto.getBrandName());
        user.setLastName("Admin");
        user.setName(format("%s %s", user.getFirstName(), user.getLastName()));
        user.setCountry(config.getString("application.countries.default"));
        user.setCreatedOn(Date.from(Instant.now()));
        user.setUserNonProfit(BooleanUtils.toBoolean(dto.getNonProfit()));
        user.setActive(false);
        user.setEmailValidated(true);
        user.setAbsorbFees(true);
        user.setReceiveEmail(Boolean.FALSE.toString().toLowerCase(Locale.ROOT));
        user.setIsUserBrand(USER_BRAND_FLAG_TRUE);

        final SecurityRole role = new SecurityRoleHome().findByRoleName(USER_ROLE, em);
        user.getSecurityRoles().add(role);
//      FIXME Vinayak
//        em.persist(user);

        user.setUserName(generateDiemLifeBrandUsername(user));
        user.setEmail(user.getUserName() + "@diemlife.com");
//      FIXME Vinayak
//        return em.merge(user);
    }

    private BrandConfig createBrandConfig(final BrandConfigImportDTO dto, final User user) {
        final BrandConfig brandConfig = new BrandConfig();

        brandConfig.setUserId(user.getId());
        brandConfig.setUser(user);
        brandConfig.setNonProfit(user.isUserNonProfit());
        brandConfig.setFullName(dto.getBrandName());
        brandConfig.setLogoUrl(dto.getLogoUrl());
        brandConfig.setSiteUrl(dto.getSiteUrl());
        brandConfig.setOnLanding(false);
        brandConfig.setLandingOrder(Short.MAX_VALUE);

//      FIXME Vinayak
//        return em.merge(brandConfig);
        return null;
    }

    private StripeAccount createStripeEntities(final BrandConfigImportDTO dto, final User user, final BrandConfig brandConfig) {
        final StripeCustomerDAO stripeEntityDao = new StripeCustomerDAO();
        final StripeAccount stripeAccount = new StripeAccount(user);

        if (user.isUserNonProfit()) {
            Logger.info(format("Creating new nonprofit Stripe account for user '%s'", user.getEmail()));

            stripeAccount.stripeAccountId = stripeConnectService.createNonProfitAccount(user, brandConfig, user.getCountry(), dto.getIp(), dto.getAgent()).getId();
        } else {
            Logger.info(format("Creating new individual Stripe account for user '%s'", user.getEmail()));

            stripeAccount.stripeAccountId = stripeConnectService.createCustomAccount(user, user.getCountry(), dto.getIp()).getId();
        }

        Logger.info(format("Creating new Stripe customer for user '%s'", user.getEmail()));

        stripeAccount.stripeCustomerId = stripeConnectService.createCustomer(user).getId();

        Logger.info(format("Finished creating Stripe entities for user '%s'", user.getEmail()));

        return stripeEntityDao.save(stripeAccount, StripeAccount.class);
    }

    private void deleteStripeEntities(final StripeEntity entity) {
        if (entity instanceof StripeAccount) {
            final Account account = stripeConnectService.deleteAccount((StripeAccount) entity);
            if (account != null) {
                Logger.info("Deleted Stripe account with ID " + account.getId());
            }
        }
        if (entity instanceof StripeCustomer) {
            final Customer customer = stripeConnectService.deleteCustomer((StripeCustomer) entity);
            if (customer != null) {
                Logger.info("Deleted Stripe customer with ID " + customer.getId());
            }
        }
        if (entity != null) {
            new StripeCustomerDAO().delete(entity);
        }
    }

    private static String generateDiemLifeBrandUsername(final User user) {
        final Slugify slugs = new Slugify().withLowerCase(true).withTransliterator(true);
        String username = format("%s-%s-nonprofit", slugs.slugify(user.getFirstName()), user.getId());
        Integer userNameSubStr = username.length() > 21 ? 21 : username.length();

        return username.substring(0, userNameSubStr);
    }

}
