package com.diemlife.exceptions;

import com.stripe.model.BankAccount;
import com.stripe.model.StripeObjectInterface;
import com.diemlife.models.StripeAccount;
import com.diemlife.models.StripeCustomer;
import com.diemlife.models.StripeEntity;
import com.diemlife.services.StripeConnectService;
import com.diemlife.services.StripeConnectService.ExportedBankAccountData;

import static java.lang.String.format;

public class PaymentModeAlreadyExistsException extends Exception {

    private final String stripeAccountId;
    private final String stripeCustomerId;
    private final ExportedBankAccountData bankAccountData;

    public PaymentModeAlreadyExistsException(final StripeEntity entity, final StripeObjectInterface existing) {
        if (entity instanceof StripeAccount) {
            this.stripeAccountId = ((StripeAccount) entity).stripeAccountId;
            this.stripeCustomerId = ((StripeAccount) entity).stripeCustomerId;
        } else if (entity instanceof StripeCustomer) {
            this.stripeAccountId = null;
            this.stripeCustomerId = ((StripeCustomer) entity).stripeCustomerId;
        } else {
            this.stripeAccountId = null;
            this.stripeCustomerId = null;
        }
        if (existing instanceof BankAccount) {
            this.bankAccountData = ExportedBankAccountData.from((BankAccount) existing);
        } else {
            this.bankAccountData = ExportedBankAccountData.from(StripeConnectService.NullBankAccount.INSTANCE);
        }
    }

    @Override
    public String getMessage() {
        return format("Stripe user with account ID [%s] and customer ID [%s] already has assigned external account", stripeAccountId, stripeCustomerId);
    }

    public ExportedBankAccountData getBankAccountData() {
        return bankAccountData;
    }

}
