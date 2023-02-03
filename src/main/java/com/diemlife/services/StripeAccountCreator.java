package com.diemlife.services;

import com.diemlife.dao.StripeCustomerDAO;
import com.diemlife.exceptions.StripeApiCallException;
import com.diemlife.models.StripeAccount;
import com.diemlife.models.StripeCustomer;
import com.diemlife.models.StripeEntity;
import com.diemlife.models.User;
import play.Logger;
import play.db.jpa.Transactional;

import java.util.Set;

import static java.lang.String.format;

public interface StripeAccountCreator extends StripeAccountManager {

    @Transactional
    default StripeCustomer createStripeCustomer(final User user) throws StripeApiCallException {
        final StripeCustomer stripeCustomer = new StripeCustomer(user);
        Logger.info(format("Creating Stripe customer for user [%s]", user.getEmail()));
        stripeCustomer.stripeCustomerId = stripeConnectService().createCustomer(user).getId();
        Logger.info(format("Finished creating Stripe customer for user [%s]", user.getEmail()));

        final StripeCustomerDAO dao = new StripeCustomerDAO(jpaApi().em());
        final StripeEntity existingUser = dao.getByUserId(user.getId(), StripeEntity.class);
        if (existingUser instanceof StripeCustomer) {
            Logger.warn(format("Stripe customer with ID [%s] already exists for user [%s]", existingUser.id, user.getEmail()));

            return (StripeCustomer) existingUser;
        } else {
            return dao.save(stripeCustomer, StripeCustomer.class);
        }
    }

    @Transactional
    default StripeAccount createStripeAccount(final User user, final String country, final String ip) throws StripeApiCallException {
        final Set<String> stripeSupportedCountries = stripeConnectService().retrieveSupportedCountriesCodes();
        if (stripeSupportedCountries.contains(country)) {
            final StripeCustomerDAO dao = new StripeCustomerDAO(jpaApi().em());
            final StripeEntity existingUser = dao.getByUserId(user.getId(), StripeEntity.class);
            if (existingUser instanceof StripeAccount) {
                Logger.warn(format("Stripe account with ID [%s] already exists for user [%s] and country [%s]", existingUser.id, user.getEmail(), country));

                return (StripeAccount) existingUser;
            } else if (existingUser instanceof StripeCustomer) {
                Logger.info(format("Creating custom Stripe account for existing customer [%s]", user.getEmail()));

                final StripeAccount stripeAccount = dao.changeStripeEntityType(existingUser.id, StripeAccount.class);
                stripeAccount.stripeAccountId = stripeConnectService().createCustomAccount(user, country, ip).getId();

                return dao.save(stripeAccount, StripeAccount.class);
            } else {
                final StripeAccount stripeAccount = new StripeAccount(user);
                Logger.info(format("Creating new custom Stripe account for user [%s]", user.getEmail()));
                stripeAccount.stripeAccountId = stripeConnectService().createCustomAccount(user, country, ip).getId();
                Logger.info(format("Creating new Stripe customer for user [%s]", user.getEmail()));
                stripeAccount.stripeCustomerId = stripeConnectService().createCustomer(user).getId();
                Logger.info(format("Finished creating Stripe entities for user [%s]", user.getEmail()));
                return dao.save(stripeAccount, StripeAccount.class);
            }
        } else {
            Logger.warn(format("Skipping Stripe account creation for user [%s] and unsupported country [%s]", user.getEmail(), country));

            return null;
        }
    }

}
