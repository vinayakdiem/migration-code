package com.diemlife.services;

import com.diemlife.dto.PersonalInfoDTO;
import exceptions.StripeApiCallException;
import com.diemlife.models.StripeAccount;
import com.diemlife.models.StripeCustomer;
import models.User;
import play.Logger;

import static java.lang.String.format;

public interface StripeAccountUpdater extends StripeAccountManager {

    default void updateStripeAccountEntity(final User user,
                                           final StripeAccount stripeEntity,
                                           final PersonalInfoDTO personalInfo) throws StripeApiCallException {
        stripeConnectService().updateConnectAccount(user, stripeEntity, personalInfo);

        Logger.info(format("Stripe account updated for user [%s]", user.getEmail()));
    }

    default void updateStripeCustomerEntity(final User user,
                                            final StripeCustomer stripeEntity) throws StripeApiCallException {
        stripeConnectService().updateCustomer(user, stripeEntity);

        Logger.info(format("Stripe customer updated for user [%s]", user.getEmail()));
    }

}
