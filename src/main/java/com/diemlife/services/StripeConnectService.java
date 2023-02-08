package com.diemlife.services;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.google.common.collect.ImmutableMap;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.Balance;
import com.stripe.model.Balance.Money;
import com.stripe.model.Balance.Money.SourceTypes;
import com.stripe.model.BankAccount;
import com.stripe.model.Card;
import com.stripe.model.Charge;
import com.stripe.model.ChargeCollection;
import com.stripe.model.CountrySpec;
import com.stripe.model.Coupon;
import com.stripe.model.Customer;
import com.stripe.model.ExternalAccount;
import com.stripe.model.ExternalAccountCollection;
import com.stripe.model.Invoice;
import com.stripe.model.Order;
import com.stripe.model.PaymentSource;
import com.stripe.model.PaymentSourceCollection;
import com.stripe.model.Payout;
import com.stripe.model.Plan;
import com.stripe.model.Price;
import com.stripe.model.PriceCollection;
import com.stripe.model.Product;
import com.stripe.model.Sku;
import com.stripe.model.SkuCollection;
import com.stripe.model.Subscription;
import com.stripe.model.Token;
import com.stripe.net.RequestOptions;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountCreateParams.BusinessProfile;
import com.stripe.param.AccountCreateParams.Capabilities;
import com.stripe.param.AccountCreateParams.Capabilities.CardPayments;
import com.stripe.param.AccountCreateParams.Capabilities.Transfers;
import com.stripe.param.AccountCreateParams.Company;
import com.stripe.param.AccountCreateParams.Company.Address;
import com.stripe.param.AccountCreateParams.TosAcceptance;
import com.typesafe.config.Config;
import com.diemlife.constants.PaymentMode;
import com.diemlife.constants.Util;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dao.StripeOrderDAO;
import com.diemlife.dto.ChargeCreationDTO;
import com.diemlife.dto.ChargeListRequestDTO;
import com.diemlife.dto.CollectionRequestDTO;
import com.diemlife.dto.InvoiceRequestStripeDTO;
import com.diemlife.dto.PaymentSourceListRequestDTO;
import com.diemlife.dto.PersonalInfoDTO;
import com.diemlife.dto.PlanCreationDTO;
import com.diemlife.dto.PlanCreationDTO.Interval;
import com.diemlife.dto.StripeAccountBaseDTO.BusinessProfileDTO;
import com.diemlife.dto.StripeAccountBaseDTO.TosAcceptanceDTO;
import com.diemlife.dto.StripeAccountCreationDTO;
import com.diemlife.dto.StripeAccountUpdateDTO;
import com.diemlife.dto.StripeAddressDTO;
import com.diemlife.dto.StripeBankVerificationDTO;
import com.diemlife.dto.StripeChargeDestinationDTO;
import com.diemlife.dto.StripeCustomerDTO;
import com.diemlife.dto.StripeDTO;
import com.diemlife.dto.StripeIndividualDTO;
import com.diemlife.dto.StripeIndividualDTO.DateOfBirthDTO;
import com.diemlife.dto.StripeOrderDTO;
import com.diemlife.dto.StripeOrderItemDTO;
import com.diemlife.dto.StripePayoutDTO;
import com.diemlife.dto.StripeShippingDTO;
import com.diemlife.dto.SubscriptionCreationDTO;
import com.diemlife.exceptions.PaymentModeAlreadyExistsException;
import com.diemlife.exceptions.StripeApiCallException;
import com.diemlife.exceptions.StripeInvalidPostalCodeException;
import com.diemlife.models.BrandConfig;
import com.diemlife.models.FundraisingLink;
import com.diemlife.models.Happening;
import com.diemlife.models.Quests;
import com.diemlife.models.StripeAccount;
import com.diemlife.models.StripeCustomer;
import com.diemlife.models.User;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import play.Logger;
import play.db.jpa.JPAApi;
import play.cache.CacheApi;
import play.cache.Cached;
import play.cache.NamedCache;
import com.diemlife.utils.FeeUtility;
import com.diemlife.utils.StripeUtils;
import com.diemlife.utils.TransactionBreakdown;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.stripe.param.AccountCreateParams.BusinessType.NON_PROFIT;
import static com.stripe.param.AccountCreateParams.Type.CUSTOM;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;
import static play.mvc.Http.Status.NOT_FOUND;
import static com.diemlife.utils.StripeUtils.loadEntireCollection;
import static com.diemlife.utils.StripeUtils.logStripeError;

@Service
public class StripeConnectService {

    private static final String CONF_STRIPE_API_KEY = "stripe.api.key";

    private static final String PAYMENT_SOURCE_OBJECT_CARD = "card";
    private static final String PAYMENT_SOURCE_OBJECT_BANK_ACCOUNT = "bank_account";
    private static final String ORDER_ITEM_TYPE_SKU = "sku";
    private static final String TRANSACTION_STATUS_FAILED = "failed";
    private static final String BANK_ACCOUNT_STATUS_VERIFIED = "verified";
    private static final String PAYOUT_METHOD_STANDARD = "standard";
    private static final String ACCOUNT_VERIFICATION_FIELDS_NEEDED = "fields_needed";
    private static final List<String> PAYMENT_SOURCE_TYPES = asList("card", "bankAccount");
    private static final String DESC_DIEM_LIFE_BACKING = "DIEMLIFE BACKING";
    private static final String DESC_DIEM_LIFE_FUNDRAISING = "DIEMLIFE FUNDRAISE";

    private static final String CHARGE_GROUP_FUNDRAISING_TEMPLATE = "FundraiserUserID[%s]-QuestID[%s]";
    private static final String CHARGE_GROUP_EVENT_TEMPLATE = "EventTickets-QuestID[%s]";

    private static final String FUNDRAISER_CHARGES_CACHE_KEY = "fundraiser-charges-%s-%s-%s";

    @Autowired
    private Config config;
    
    @Autowired
    private String stripeApiKey = config.getString(CONF_STRIPE_API_KEY);
    
    @Autowired
    private FeeUtility feeUtility;
    
    @Autowired
    private UserPaymentFeeService userPaymentFeeService;
    
    @Autowired
    private CacheApi transactionsCache;
    
    @Autowired
    private CacheApi creditCardsCache;
    
    @Autowired
    private CacheApi bankAccountsCache;

    public Account deleteAccount(final StripeAccount entity) {
        final RequestOptions options = platformRequestOptions();
        final Account stripeAccount = getAccount(entity);
        if (stripeAccount == null) {
            return null;
        } else {
            try {
                return stripeAccount.delete(options);
            } catch (final StripeException e) {
                throw new StripeApiCallException(format("Unable to delete Stripe account with ID [%s]", entity.stripeAccountId), e);
            }
        }
    }

    public Customer deleteCustomer(final StripeCustomer entity) {
        final RequestOptions options = platformRequestOptions();
        final Customer stripeCustomer = getCustomer(entity);
        if (stripeCustomer == null) {
            return null;
        } else {
            try {
                return stripeCustomer.delete(options);
            } catch (final StripeException e) {
                throw new StripeApiCallException(format("Unable to delete Stripe customer with ID [%s]", entity.stripeCustomerId), e);
            }
        }
    }

    public Account createNonProfitAccount(final User user,
                                          final BrandConfig brandConfig,
                                          final String country,
                                          final String ip,
                                          final String browser) {

        final AccountCreateParams payload = AccountCreateParams.builder()
                .setType(CUSTOM)
                .setBusinessType(NON_PROFIT)
                .setCountry(country)
                .setEmail(user.getEmail())
                .setBusinessProfile(BusinessProfile.builder()
                        .setName(brandConfig.getFullName())
                        .setUrl(brandConfig.getSiteUrl())
                        .build())
                .setCompany(Company.builder()
                        .setName(brandConfig.getFullName())
                        .setAddress(Address.builder()
                                .setCountry(user.getCountry())
                                .setPostalCode(user.getZip())
                                .build())
                        .build())
                .setTosAcceptance(TosAcceptance.builder()
                        .setDate(Calendar.getInstance().getTimeInMillis() / 1000L)
                        .setIp(ip)
                        .setUserAgent(browser)
                        .build())
                .setCapabilities(Capabilities.builder()
                        .setCardPayments(CardPayments.builder()
                                .setRequested(true)
                                .build())
                        .setTransfers(Transfers.builder()
                                .setRequested(true)
                                .build())
                        .build())
                .build();

        try {
            return Account.create(payload, platformRequestOptions());
        } catch (final StripeException e) {
            if (e instanceof InvalidRequestException) {
                final InvalidRequestException realCause = (InvalidRequestException) e;
                final String param = realCause.getParam();
                final String message = realCause.getMessage();

                throw new StripeApiCallException(format("Unable to create Stripe Account with email [%s], %s - %s", user.getEmail(), message, param), e);
            } else {
                throw new StripeApiCallException(format("Unable to create Stripe Account with email [%s]", user.getEmail()), e);
            }
        }
    }

    public Account createCustomAccount(final User user,
                                       final String country,
                                       final String ip) throws StripeApiCallException {
        final StripeAddressDTO address = new StripeAddressDTO();
        address.country = upperCase(country);
        address.postalCode = user.getZip();

        final StripeIndividualDTO individual = new StripeIndividualDTO();
        individual.firstName = user.getFirstName();
        individual.lastName = user.getLastName();
        individual.email = user.getEmail();
        individual.address = address;

        final TosAcceptanceDTO tosAcceptance = new TosAcceptanceDTO();
        tosAcceptance.date = Calendar.getInstance().getTimeInMillis() / 1000L;
        tosAcceptance.ip = ip;

        final StripeAccountCreationDTO request = new StripeAccountCreationDTO();
        request.country = country;
        request.email = user.getEmail();
        request.individual = individual;
        request.requestedCapabilities = asList("card_payments", "transfers");
        request.tosAcceptance = tosAcceptance;

        try {
            return Account.create(request.toMap(), platformRequestOptions());
        } catch (final StripeException e) {
            if (e instanceof InvalidRequestException) {
                final InvalidRequestException realCause = (InvalidRequestException) e;
                final String param = realCause.getParam();
                final String message = realCause.getMessage();
                if (StringUtils.contains(param, "[postal_code]")) {
                    final StripeInvalidPostalCodeException throwable = new StripeInvalidPostalCodeException(message, e);

                    Logger.error(format("Unable to create Stripe Account for user '%s' - invalid postal code '%s'", user.getEmail(), user.getZip()), throwable);

                    throw throwable;
                }
            }
            throw new StripeApiCallException(format("Unable to create Stripe Account with email [%s]", user.getEmail()), e);
        }
    }

    public Customer createCustomer(final User user) throws StripeApiCallException {
        final StripeCustomerDTO stripeCustomer = new StripeCustomerDTO();
        stripeCustomer.email = user.getEmail();
        stripeCustomer.description = user.getName();

        try {
            return Customer.create(stripeCustomer.toMap(), platformRequestOptions());
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to create Stripe Customer with email [%s]", user.getEmail()), e);
        }
    }

    private Customer createCustomerForSubscription(final StripeAccount merchant,
                                                   final StripeCustomer buyer,
                                                   final Token token) throws StripeApiCallException {
        final StripeCustomerDTO customerRequest = new StripeCustomerDTO();
        customerRequest.email = buyer.user.getEmail();
        customerRequest.description = "Copy of platform customer " + buyer.stripeCustomerId;
        customerRequest.source = token.getId();
        customerRequest.metadata.put("platformCustomerId", buyer.stripeCustomerId);

        try {
            return Customer.create(customerRequest.toMap(), connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to create Stripe Customer with email [%s]", buyer.user.getEmail()), e);
        }
    }

    private Customer getCustomer(final StripeCustomer customer) throws StripeApiCallException {
        if (customer == null) {
            throw new IllegalArgumentException("StripeCustomer ID is required to retrieve the Stripe Customer");
        }
        try {
            return Customer.retrieve(customer.stripeCustomerId, platformRequestOptions());
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to retrieve Stripe Customer with ID [%s]", customer.stripeCustomerId), e);
        }
    }

    private Account getAccount(final StripeAccount account) throws StripeApiCallException {
        if (account == null) {
            throw new IllegalArgumentException("StripeCustomer ID is required to retrieve the Stripe Account");
        }
        try {
            return Account.retrieve(account.stripeAccountId, platformRequestOptions());
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to retrieve Stripe Account with ID [%s]", account.stripeAccountId), e);
        }
    }

    private List<Card> getCustomerCreditCards(final Customer customer, final String lastFour) throws StripeApiCallException {
        return loadEntireCollection(request -> Optional.ofNullable(customer.getSources()).map(sources -> {
            try {
                return sources.list(request.toMap(), platformRequestOptions());
            } catch (final StripeException e) {
                throw new StripeApiCallException(format("Unable to list customer's payment sources for Stripe Customer with ID [%s]", customer.getId()), e);
            }
        }).orElse(new PaymentSourceCollection()), () -> new PaymentSourceListRequestDTO(PAYMENT_SOURCE_OBJECT_CARD), 10).stream()
                .filter(source -> source instanceof Card)
                .map(Card.class::cast)
                .filter(card -> isBlank(lastFour) || StringUtils.equals(card.getLast4(), lastFour))
                .collect(toList());
    }

    private BankAccount getFirstBankAccount(final Customer customer) {
        if (customer == null || isBlank(customer.getId())) {
            return null;
        }
        final BankAccount bankAccount = bankAccountsCache.getOrElse(customer.getId(), () -> getNonNullFirstBankAccountFromRemote(getCustomerBankAccounts(customer)));
        return bankAccount == NullBankAccount.INSTANCE ? null : bankAccount;
    }

    private BankAccount getFirstBankAccount(final Account account) {
        if (account == null || isBlank(account.getId())) {
            return null;
        }
        final BankAccount bankAccount = bankAccountsCache.getOrElse(account.getId(), () -> getNonNullFirstBankAccountFromRemote(getConnectedAccountBankAccounts(account)));
        return bankAccount == NullBankAccount.INSTANCE ? null : bankAccount;
    }

    private BankAccount getNonNullFirstBankAccountFromRemote(final PaymentSourceCollection customerBankAccounts) {
        if (customerBankAccounts == null) {
            return NullBankAccount.INSTANCE;
        }
        final List<PaymentSource> bankAccounts = customerBankAccounts.getData();
        if (Util.isEmpty(bankAccounts)) {
            return NullBankAccount.INSTANCE;
        } else {
            final PaymentSource bankAccount = bankAccounts.iterator().next();
            if (bankAccount instanceof BankAccount) {
                return (BankAccount) bankAccount;
            } else {
                throw new IllegalStateException(format("Payment source is not a bank accounts %s", bankAccount));
            }
        }
    }

    private BankAccount getNonNullFirstBankAccountFromRemote(final ExternalAccountCollection customerBankAccounts) {
        if (customerBankAccounts == null) {
            return NullBankAccount.INSTANCE;
        }
        final List<ExternalAccount> bankAccounts = customerBankAccounts.getData();
        if (Util.isEmpty(bankAccounts)) {
            return NullBankAccount.INSTANCE;
        } else {
            final ExternalAccount bankAccount = bankAccounts.iterator().next();
            if (bankAccount instanceof BankAccount) {
                return (BankAccount) bankAccount;
            } else {
                throw new IllegalStateException(format("Payment source is not a bank accounts %s", bankAccount));
            }
        }
    }


    public List<ExportedCreditCardData> exportCustomerCreditCards(final StripeCustomer customer) throws StripeApiCallException {
        return getCustomerCreditCards(getCustomer(customer), null).stream().map(ExportedCreditCardData::from).collect(toList());
    }

    public ExportedCreditCardData exportCustomerCreditCard(final StripeCustomer customer, final String lastFour) throws StripeApiCallException {
        return getCustomerCreditCards(getCustomer(customer), lastFour).stream().map(ExportedCreditCardData::from).findFirst().orElse(null);
    }

    public Card saveNewCreditCard(final StripeCustomer customer, final String token) throws StripeApiCallException {
        final Customer stripeCustomer = getCustomer(customer);
        final PaymentSourceDTO request = new PaymentSourceDTO();
        request.source = token;
        final PaymentSource externalAccount = createPaymentSourceForCustomer(stripeCustomer, request);
        if (externalAccount instanceof Card) {
            Logger.info(format("Successfully saved new credit card for customer [%s]", customer.stripeCustomerId));

            return (Card) externalAccount;
        } else {
            throw new IllegalStateException(format("Error saving external account for customer [%s]", customer.stripeCustomerId));
        }
    }

    public void deleteExistingCreditCard(final StripeCustomer customer, final String lastFour) throws StripeApiCallException {
        final List<Card> cards = getCustomerCreditCards(getCustomer(customer), lastFour);
        if (Util.isEmpty(cards)) {
            throw new IllegalStateException(format("Stripe customer [%s] doesn't have any credit card with last 4 digits of [%s]", customer.stripeCustomerId, lastFour));
        }
        cards.forEach(card -> {
            try {
                card.delete(platformRequestOptions());

                Logger.info(format("Successfully deleted customer's [%s] credit card ending with '%s'", customer.stripeCustomerId, lastFour));
            } catch (final StripeException e) {
                throw new StripeApiCallException(format("Unable to delete credit card with ID [%s]", card.getId()), e, true);
            } finally {
                creditCardsCache.remove(customer.stripeCustomerId);
            }
        });
    }

    private Balance getBalance(final StripeAccount customer) throws StripeApiCallException {
        try {
            return Balance.retrieve(connectAccountRequestOptions(customer));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to retrieve balance for Stripe Customer with ID [%s]", customer.stripeCustomerId), e, true);
        }
    }

    public ExportedBalance retrieveAvailableBalance(final StripeAccount customer, final String currency) throws StripeApiCallException {
        final Balance balance = getBalance(customer);
        final Function<List<Balance.Money>, Long> sum = (monies -> monies.stream()
                .filter(money -> equalsIgnoreCase(currency, money.getCurrency()))
                .collect(Collectors.summarizingLong(Balance.Money::getAmount))
                .getSum());
        final ExportedBalance result = new ExportedBalance();
        result.available = sum.apply(balance.getAvailable());
        result.pending = sum.apply(balance.getPending());
        return result;
    }

    private Charge getCharge(final String chargeId, final StripeAccount beneficiary) throws StripeApiCallException {
        try {
            return Charge.retrieve(chargeId, ImmutableMap.of("expand", singletonList("transfer")), platformRequestOptions());
        } catch (final StripeException e) {
            if (e instanceof InvalidRequestException && e.getStatusCode().equals(NOT_FOUND)) {
                return getChargeFromBeneficiary(chargeId, beneficiary);
            } else {
                throw new StripeApiCallException(format("Unable to retrieve charge with ID [%s]", chargeId), e);
            }
        }
    }

    private Charge getChargeFromBeneficiary(final String chargeId, final StripeAccount beneficiary) throws StripeApiCallException {
        try {
            return Charge.retrieve(chargeId, ImmutableMap.of("expand", singletonList("balance_transaction")), connectAccountRequestOptions(beneficiary));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to retrieve charge with ID [%s] from beneficiary [%s]", chargeId, beneficiary.stripeAccountId), e);
        }
    }

    public Charge retrieveChargeInformation(final String chargeId, final StripeAccount beneficiary, final boolean paidOnly) throws StripeApiCallException {
        final Charge charge = transactionsCache.getOrElse(chargeId, () -> getCharge(chargeId, beneficiary));
        if (charge == null || (paidOnly && isNotTrue(charge.getPaid()))) return null;

        return charge;
    }

    public List<Charge> retrieveCharges(final Integer questId, final Integer fundraiserUserId, final StripeAccount beneficiary) throws StripeApiCallException {
        return transactionsCache.getOrElse(format(FUNDRAISER_CHARGES_CACHE_KEY,
                questId,
                fundraiserUserId,
                beneficiary.user.getId()), () -> getCharges(questId, fundraiserUserId, beneficiary));
    }

    private List<Charge> getCharges(final Integer questId, final Integer fundraiserId, final StripeAccount beneficiary) throws StripeApiCallException {
        final String chargeGroup = format(CHARGE_GROUP_FUNDRAISING_TEMPLATE, fundraiserId, questId);
        final List<Charge> accountCharges = StripeUtils.loadEntireCollection(request -> {
            try {
                return Charge.list(request.toMap(), connectAccountRequestOptions(beneficiary));
            } catch (final StripeException e) {
                StripeUtils.logStripeError(e.getMessage(), e);
                return new ChargeCollection();
            }
        }, () -> new ChargeListRequestDTO(chargeGroup, "data.balance_transaction"), 50);
        final List<Charge> platformCharges = StripeUtils.loadEntireCollection(request -> {
            try {
                return Charge.list(request.toMap(), platformRequestOptions());
            } catch (final StripeException e) {
                // Suppress stacktrace for known Stripe bug
                String eMsg = e.getMessage();
                if (eMsg.indexOf("Too many charges with transfer group") == -1) {
                    StripeUtils.logStripeError(eMsg, e);
                } else {
                    Logger.warn("getCharges - encountered Stripe > 100 transactions bug");
                }
                return new ChargeCollection();
            }
        }, () -> new ChargeListRequestDTO(chargeGroup, "data.transfer"), 50);
        final List<Charge> result = new ArrayList<>();
        result.addAll(accountCharges);
        result.addAll(platformCharges);
        return result;
    }

    public Payout retrievePayoutInformation(final String payoutId, final StripeAccount merchant) throws StripeApiCallException {
        try {
            return Payout.retrieve(payoutId, ImmutableMap.of("expand", singletonList("destination")), connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to retrieve payout with ID [%s]", payoutId), e);
        }
    }

    public ExportedProduct retrieveProduct(final StripeAccount merchant, final String productId) throws StripeApiCallException {
        if (isBlank(productId)) {
            return null;
        }
        final Product product = retrieveStripeProduct(merchant, productId);
        try {
            // try and grab things by price, fall back to sku on failure
            final PriceCollection prices = retrievePricesForProduct(merchant, productId);
            if (!prices.getData().isEmpty()) {
                return ExportedProduct.fromPrices(product, feeUtility, prices);
            } else {
                Logger.warn("retrieveProduct - no prices found, falling back to leagacy SKUs.");
                final SkuCollection skus = retrieveSKUsForProduct(merchant, productId);
                return ExportedProduct.fromSkus(product, feeUtility, skus);
            }
        } catch (final StripeApiCallException e) {
            // Note: there was no try-catch here before the Price changes.  Perhaps it is assumed that the Prices code could throw?
            // TODO: remove this code later if it is not needed.
            Logger.error("retrieveProduct - error in API call: " + e.toString());
            final SkuCollection skus = retrieveSKUsForProduct(merchant, productId);
            return ExportedProduct.fromSkus(product, feeUtility, skus);
        }
    }

    private Product retrieveStripeProduct(final StripeAccount merchant, final String productId) throws StripeApiCallException {
        try {
            return Product.retrieve(productId, connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to retrieve Product with Id [%s]", productId), e);
        }
    }

    private SkuCollection retrieveSKUsForProduct(final StripeAccount merchant, final String productId) throws StripeApiCallException {
        try {
            final Map<String, Object> skuParams = new HashMap<>();
            skuParams.put("product", productId);
            return Sku.list(skuParams, connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to retrieve SKUs with Product Id [%s]", productId), e);
        }
    }

    private PriceCollection retrievePricesForProduct(final StripeAccount merchant, final String productId) throws StripeApiCallException {
        try {
            final Map<String, Object> params = new HashMap<>();

            params.put("product", productId);
            params.put("type", "one_time");
            params.put("active", true);

            return Price.list(params, connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to retrieve SKUs with Product Id [%s]", productId), e);
        }
    }

    public ExportedCoupon exportCoupon(final StripeAccount merchant, final String couponCode) throws StripeApiCallException {
        return ExportedCoupon.from(retrieveCoupon(merchant, couponCode));
    }

    private Coupon retrieveCoupon(final StripeAccount merchant, final String couponCode) throws StripeApiCallException {
        try {
            return Coupon.retrieve(upperCase(couponCode), connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to retrieve coupon with code [%s]", couponCode), e);
        }
    }

    @Cached(key = "Service.SupportedCountriesCodes.Stripe")
    public Set<String> retrieveSupportedCountriesCodes() {
        return StripeUtils.loadEntireCollection(request -> {
            try {
                return CountrySpec.list(request.toMap(), platformRequestOptions());
            } catch (final StripeException e) {
                StripeUtils.logStripeError(e.getMessage(), e);
                return null;
            }
        }, CollectionRequestDTO::new, 50)
                .stream()
                .map(CountrySpec::getId)
                .collect(Collectors.toSet());
    }

    public TransactionBreakdown transactionBreakdown(final StripeAccount merchant,
                                                     final Map<String, Integer> orderItems,
                                                     final PaymentMode paymentMode,
                                                     final String couponCode) {
        return feeUtility.calculateTicketsFees(
                // lookup by price, fallback to sku
                itemId -> Optional.ofNullable(fromPrice(merchant, itemId)).orElse(fromSku(merchant, itemId)),
                code -> fromCoupon(merchant, code),
                orderItems,
                upperCase(couponCode),
                paymentMode
        );
    }

    @Nullable
    protected ExportedProductVariant fromSku(final StripeAccount merchant, final String itemId) {
        try {
            return ExportedProductVariant.fromSku(Sku.retrieve(itemId, connectAccountRequestOptions(merchant)));
        } catch (final StripeException e) {
            Logger.warn("fromSku - id not found: " + itemId + ". Is it a price?");
            return null;
        }
    }

    @Nullable
    protected ExportedProductVariant fromPrice(final StripeAccount merchant, final String itemId) {
        try {
            return ExportedProductVariant.fromPrice(Price.retrieve(itemId, connectAccountRequestOptions(merchant)));
        } catch (final StripeException e) {
            Logger.warn("fromPrice - id not found: " + itemId + ". Is it a sku?");
            return null;
        }
    }

    @Nullable
    protected ExportedCoupon fromCoupon(final StripeAccount merchant, final String code) {
        try {
            return ExportedCoupon.from(Coupon.retrieve(code, connectAccountRequestOptions(merchant)));
        } catch (final StripeException e) {
            Logger.warn("Error retrieving Coupon with code " + code);
            return null;
        }
    }

    public TransactionBreakdown transactionBreakdown(final TicketsPurchaseOrder ticketsPurchaseOrder) {
        return feeUtility.calculateTicketsFees(
                skuId -> Stream.of(ticketsPurchaseOrder.product.variants).filter(v -> StringUtils.equals(skuId, v.id)).findFirst().orElse(null),
                code -> Optional.ofNullable(ticketsPurchaseOrder.coupon).filter(c -> StringUtils.equals(code, c.code)).orElse(null),
                ticketsPurchaseOrder.getOrderItemQuantities(),
                ticketsPurchaseOrder.getCouponUsed(),
                ticketsPurchaseOrder.getPaymentMode()
        );
    }

    public TransactionBreakdown transactionBreakdown(final Long amount,
                                                     final PaymentMode paymentMode,
                                                     final boolean absorbFeesBySeller,
                                                     final boolean absorbFeesByBuyer,
                                                     final User backee,
                                                     final Double tip) {
        final BigDecimal tipAmount = tip == null || tip <= 0.0D ? BigDecimal.ZERO : BigDecimal.valueOf(tip * amount).setScale(Byte.MAX_VALUE, RoundingMode.HALF_UP);
        final BigDecimal fullAmount = BigDecimal.valueOf(amount).add(tipAmount).setScale(Byte.MAX_VALUE, RoundingMode.HALF_UP);
        final BigDecimal applicationFee = BigDecimal.valueOf(userPaymentFeeService.getFeeByUser(backee)).setScale(Byte.MAX_VALUE, RoundingMode.HALF_UP);

        if (absorbFeesBySeller) {
            return feeUtility.calculateFeesIncludedInAmount(BigDecimal.valueOf(amount), tipAmount, paymentMode, applicationFee);
        } else if (absorbFeesByBuyer) {
            return feeUtility.calculateFeesOnTopOfAmount(fullAmount, tipAmount, paymentMode, applicationFee);
        } else {
            return feeUtility.calculateFeesIncludedInAmount(BigDecimal.valueOf(amount), tipAmount, paymentMode, applicationFee);
        }
    }

    public String payOrderWithPaymentSource(final StripeAccount merchant,
                                            final StripeCustomer buyer,
                                            final String billingEmail,
                                            final StripeShippingDTO billingInfo,
                                            final PurchaseOrder purchaseOrder,
                                            long questId, long eventId) throws StripeApiCallException {
        validateStripeMerchant(merchant);

        final TransactionBreakdown breakdown = feeUtility.calculateTicketsFees(
                skuId -> purchaseOrder.orderItems.get(skuId).getLeft(),
                code -> purchaseOrder.coupon,
                purchaseOrder.getOrderItemQuantities(),
                purchaseOrder.getCouponUsed(),
                purchaseOrder.getPaymentMode()
        );

        if (breakdown.brutTotal > 0) {
            return payOrderWithSourceToken(
                    merchant,
                    buyer,
                    billingEmail,
                    billingInfo,
                    purchaseOrder,
                    breakdown,
                    questId,
                    eventId
            );
        } else {
            return registerFreeOrder(
                    merchant,
                    billingEmail,
                    billingInfo,
                    purchaseOrder,
                    questId,
                    eventId
            );
        }
    }

    public String createQuestBackingSubscription(final @Nonnull StripeAccount backee,
                                                 final @Nonnull StripeCustomer backer,
                                                 final boolean backerFeesAbsorption,
                                                 final boolean backerPaysNow,
                                                 final @Nonnull Quests quest,
                                                 final @Nonnull PaymentMethod paymentMethod,
                                                 final @Nonnull String currency,
                                                 final @Nonnull Long amount,
                                                 final Double formFee) throws StripeApiCallException {
        final PlanCreationDTO planRequest = new PlanCreationDTO();
        final TransactionBreakdown breakdown = transactionBreakdown(amount, paymentMethod.paymentMode, backee.user.isAbsorbFees(), backerFeesAbsorption, backee.user, formFee);

        planRequest.amount = Long.valueOf(breakdown.brutTotal).intValue();
        planRequest.currency = currency;
        planRequest.interval = Interval.month;
        planRequest.intervalCount = 1;
        planRequest.product = new PlanCreationDTO.ServiceProduct();
        planRequest.product.name = format("Backing %s's Quest [%s]", backee.user.getName(), quest.getTitle());
        planRequest.product.statementDescriptor = "DIEMLIFE RECURR BACK";
        planRequest.product.metadata.putIfAbsent("beneficiary", backee.user.getEmail());
        planRequest.product.metadata.putIfAbsent("quest-id", quest.getId());
        planRequest.product.metadata.putIfAbsent("quest-name", quest.getTitle());
        planRequest.metadata.put("backer", backer.stripeCustomerId);
        planRequest.metadata.put("backee", backee.stripeAccountId);

        final Plan plan = createPlan(backee, planRequest);

        Logger.info(format("Successfully created subscription Plan with ID [%s] and name [%s]", plan.getId(), plan.getNickname()));

        final SubscriptionCreationDTO subscriptionRequest = new SubscriptionCreationDTO();
        subscriptionRequest.plan = plan.getId();
        subscriptionRequest.quantity = 1L;
        //TODO need platform fee
        subscriptionRequest.applicationFeePercent = feeUtility.calculatePlatformFeePercent(amount, breakdown.brutTotal, 0);

        final Token token = generatePaymentToken(backee, backer, paymentMethod);
        final Customer connectedCustomer = createCustomerForSubscription(backee, backer, token);
        subscriptionRequest.customer = connectedCustomer.getId();

        if (backerPaysNow) {
            subscriptionRequest.prorate = true;
        } else {
            final LocalDateTime monthFromNow = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay();
            subscriptionRequest.billingCycleAnchor = Timestamp.valueOf(monthFromNow).getTime() / 1000;
            subscriptionRequest.prorate = false;
        }

        final Subscription subscription = createSubscription(backee, subscriptionRequest);

        Logger.info(format("Successfully created Subscription with ID [%s], plan ID [%s] and new customer [%s]", subscription.getId(), plan.getId(), connectedCustomer.getId()));

        return subscription.getId();
    }

    private Plan createPlan(final StripeAccount merchant, final PlanCreationDTO request) throws StripeApiCallException {
        try {
            return Plan.create(request.toMap(), connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to create plan for Stripe Account with ID [%s]", merchant.stripeAccountId), e);
        }
    }

    private Subscription createSubscription(final StripeAccount merchant, final SubscriptionCreationDTO request) throws StripeApiCallException {
        try {
            return Subscription.create(request.toMap(), connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to create subscription for Stripe Account with ID [%s]", merchant.stripeAccountId), e);
        }
    }

    public Subscription getQuestBackingSubscription(final String id, final StripeAccount beneficiary) {
        try {
            return Subscription.retrieve(id, connectAccountRequestOptions(beneficiary));
        } catch (final StripeException e) {
            logStripeError(format("Unable to retrieve subscription with ID [%s] owned by [%s]", id, beneficiary.stripeAccountId), e);

            return null;
        }
    }

    public Subscription cancelQuestBackingSubscription(final String id, final StripeAccount beneficiary) throws StripeApiCallException {
        Logger.info(format("Cancelling subscription with ID [%s] for beneficiary account [%s]", id, beneficiary.stripeAccountId));

        final Subscription subscription = getQuestBackingSubscription(id, beneficiary);
        if (subscription != null) {
            try {
                return subscription.cancel(new HashMap<>(), connectAccountRequestOptions(beneficiary));
            } catch (final StripeException e) {
                logStripeError(format("Unable to  cancel subscription with ID [%s] owned by [%s]", id, beneficiary.stripeAccountId), e);
            }
        } else {
            Logger.info(format("Subscription with ID [%s] not found for beneficiary account [%s]", id, beneficiary.stripeAccountId));
        }

        return null;
    }

    public List<Charge> getInvoicesForSubscription(final String subscriptionId, final StripeAccount merchant) {
        return StripeUtils.loadEntireCollection(request -> {
            try {
                return Invoice.list(request.toMap(), connectAccountRequestOptions(merchant));
            } catch (final StripeException e) {
                StripeUtils.logStripeError(e.getMessage(), e);
                return null;
            }
        }, () -> new InvoiceRequestStripeDTO(subscriptionId, "data.charge", "data.charge.transfer", "data.charge.balance_transaction"), 50)
                .stream()
                .map(Invoice::getChargeObject)
                .collect(toList());
    }

    public String backUserWithPaymentSource(final @Nonnull StripeAccount beneficiary,
                                            final @Nullable StripeCustomer customer,
                                            final @Nonnull PaymentMethod paymentMethod,
                                            final @Nonnull String currency,
                                            final @Nonnull Long amount,
                                            final Double formFee,
                                            final long questId) throws StripeApiCallException {
        validateStripeMerchant(beneficiary);
        validateCurrency(currency);

        final Charge charge = createChargeForQuestBacking(
                beneficiary,
                customer,
                paymentMethod,
                lowerCase(currency),
                amount,
                formFee,
                questId
        );
        final String chargeStatus = charge.getStatus();
        if (TRANSACTION_STATUS_FAILED.equalsIgnoreCase(chargeStatus)) {
            throw new IllegalStateException(format("Unexpected charge status %s", chargeStatus));
        } else {
            Logger.info(format("Successfully created backing Charge with ID [%s] and beneficiary ID [%s]",
                    charge.getId(),
                    beneficiary.stripeAccountId));

            return charge.getId();
        }
    }

    public String raiseFundsForQuestWithPaymentSource(final @Nonnull StripeAccount beneficiary,
                                                      final @Nullable StripeCustomer customer,
                                                      final @Nonnull FundraisingLink fundraisingLink,
                                                      final @Nonnull PaymentMethod paymentMethod,
                                                      final @Nonnull String currency,
                                                      final @Nonnull Long amount,
                                                      final Double formFee,
                                                      final long questId) throws StripeApiCallException {
        validateStripeMerchant(beneficiary);
        validateCurrency(currency);

        final Charge charge = createChargeForQuestFundraising(
                beneficiary,
                customer,
                fundraisingLink,
                paymentMethod,
                lowerCase(currency),
                amount,
                formFee,
                questId
        );
        final String chargeStatus = charge.getStatus();
        if (TRANSACTION_STATUS_FAILED.equalsIgnoreCase(chargeStatus)) {
            throw new IllegalStateException(format("Unexpected charge status %s", chargeStatus));
        } else {
            Logger.info(format("Successfully created fundraising Charge with ID [%s] and beneficiary ID [%s]",
                    charge.getId(),
                    beneficiary.stripeAccountId));

            transactionsCache.remove(format(FUNDRAISER_CHARGES_CACHE_KEY,
                    fundraisingLink.quest.getId(),
                    fundraisingLink.fundraiser.getId(),
                    beneficiary.user.getId()));
            return charge.getId();
        }
    }

    private String getPaymentSource(final StripeCustomer buyer,
                                    final PaymentMethod paymentMethod) throws StripeApiCallException {
        switch (paymentMethod.paymentMode) {
            case CreditCardToken:
                if (paymentMethod.save) {
                    return saveNewCreditCard(buyer, paymentMethod.token).getId();
                } else {
                    return paymentMethod.token;
                }
            case CreditCard:
                final List<Card> cards = getCustomerCreditCards(getCustomer(buyer), paymentMethod.lastFour);
                if (Util.isEmpty(cards)) {
                    throw new IllegalStateException(format("Customer [%s] doesn't have any saved credit card ending with '%s'", buyer.stripeCustomerId, paymentMethod.lastFour));
                }
                return cards.iterator().next().getId();
            case BankAccount:
                final BankAccount account = getFirstBankAccount(getCustomer(buyer));
                if (account == null) {
                    throw new IllegalStateException(format("Customer %s doesn't have any saved bank accounts", buyer.id));
                }
                return account.getId();
            default:
                throw new IllegalArgumentException(format("Unknown payment mode %s", paymentMethod.paymentMode));
        }
    }

    private Token generatePaymentToken(final StripeAccount merchant,
                                       final StripeCustomer buyer,
                                       final PaymentMethod paymentMethod) throws StripeApiCallException {
        switch (paymentMethod.paymentMode) {
            case CreditCardToken:
                final CreditCardPaymentTokenDTO newCreditCardTokenRequest = new CreditCardPaymentTokenDTO();
                newCreditCardTokenRequest.card = saveNewCreditCard(buyer, paymentMethod.token).getId();
                newCreditCardTokenRequest.customer = buyer.stripeCustomerId;

                return createToken(merchant, newCreditCardTokenRequest);
            case CreditCard:
                final List<Card> cards = getCustomerCreditCards(getCustomer(buyer), paymentMethod.lastFour);
                if (Util.isEmpty(cards)) {
                    throw new IllegalStateException(format("Customer [%s] doesn't have any saved credit card ending with '%s'", buyer.stripeCustomerId, paymentMethod.lastFour));
                }

                final CreditCardPaymentTokenDTO creditCardTokenRequest = new CreditCardPaymentTokenDTO();
                creditCardTokenRequest.card = cards.iterator().next().getId();
                creditCardTokenRequest.customer = buyer.stripeCustomerId;

                return createToken(merchant, creditCardTokenRequest);
            case BankAccount:
                final BankAccount account = getFirstBankAccount(getCustomer(buyer));
                if (account == null) {
                    throw new IllegalStateException(format("Customer %s doesn't have any saved bank accounts", buyer.id));
                }

                final BankAccountPaymentTokenDTO bankAccountTokenRequest = new BankAccountPaymentTokenDTO();
                bankAccountTokenRequest.bankAccount = account.getId();
                bankAccountTokenRequest.customer = buyer.stripeCustomerId;

                return createToken(merchant, bankAccountTokenRequest);
            default:
                throw new IllegalArgumentException(format("Unknown payment mode %s", paymentMethod.paymentMode));
        }
    }

    private Token createToken(final StripeAccount merchant, final PaymentTokenDTO request) throws StripeApiCallException {
        try {
            return Token.create(request.toMap(), connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to create payment token for customer with ID [%s]", request.customer), e);
        }
    }

    public ExportedBankAccountData createFirstBankAccountForCustomer(final StripeCustomer customer,
                                                                     final String token) throws StripeApiCallException, PaymentModeAlreadyExistsException {
        final Customer stripeCustomer = getCustomer(customer);
        final PaymentSourceCollection existingBankAccounts = getCustomerBankAccounts(stripeCustomer);
        if (!Util.isEmpty(existingBankAccounts.getData())) {
            throw new PaymentModeAlreadyExistsException(customer, existingBankAccounts.getData().iterator().next());
        }

        final PaymentSourceDTO stripeExternalAccount = new PaymentSourceDTO();

        stripeExternalAccount.source = token;

        final PaymentSource bankAccount = createPaymentSourceForCustomer(stripeCustomer, stripeExternalAccount);

        Logger.info(format("External bank account added for Stripe customer %s", customer.stripeCustomerId), bankAccount);

        return ExportedBankAccountData.from((BankAccount) bankAccount);
    }

    private PaymentSource createPaymentSourceForCustomer(final Customer customer,
                                                         final PaymentSourceDTO request) throws StripeApiCallException {
        try {
            return customer.getSources().create(request.toMap(), platformRequestOptions());
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to create payment source for Stripe customer with ID [%s] due to code [%s]", customer.getId(), e.getCode()), e, true);
        } finally {
            creditCardsCache.remove(customer.getId());
            bankAccountsCache.remove(customer.getId());
        }
    }

    public ExportedBankAccountData createFirstBankAccountForConnectedAccount(final StripeAccount customer,
                                                                             final String token) throws StripeApiCallException, PaymentModeAlreadyExistsException {
        final Account account = getAccount(customer);
        final ExternalAccountCollection bankAccounts = getConnectedAccountBankAccounts(account);
        if (!Util.isEmpty(bankAccounts.getData())) {
            throw new PaymentModeAlreadyExistsException(customer, bankAccounts.getData().iterator().next());
        }

        final BankAccountCreationDTO request = new BankAccountCreationDTO();

        request.externalAccount = token;

        final ExternalAccount bankAccount = createPayoutTargetForAccount(account, request);

        Logger.info(format("External bank account added for Stripe account %s", customer.stripeAccountId));

        return ExportedBankAccountData.from((BankAccount) bankAccount);
    }

    private ExternalAccount createPayoutTargetForAccount(final Account account, final BankAccountCreationDTO request) throws StripeApiCallException {
        try {
            return account.getExternalAccounts().create(request.toMap(), platformRequestOptions());
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to create payout target for Stripe account with ID [%s]", account.getId()), e);
        } finally {
            creditCardsCache.remove(account.getId());
            bankAccountsCache.remove(account.getId());
        }
    }

    public ExportedBankAccountData retrieveFirstBankAccountForConnectedAccount(final StripeAccount merchant) throws StripeApiCallException {
        return ExportedBankAccountData.from(getFirstBankAccount(getAccount(merchant)));
    }

    public ExportedBankAccountData retrieveFirstBankAccountForCustomer(final StripeCustomer customer) throws StripeApiCallException {
        return ExportedBankAccountData.from(getFirstBankAccount(getCustomer(customer)));
    }

    public void deleteFirstBankAccountForCustomer(final StripeCustomer customer) throws StripeApiCallException {
        final BankAccount bankAccount = getFirstBankAccount(getCustomer(customer));
        if (bankAccount == null) {
            throw new IllegalStateException(format("Stripe customer with ID [%s] doesn't have any bank accounts to delete", customer.stripeCustomerId));
        }
        try {
            bankAccount.delete(platformRequestOptions());
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to delete bank account with ID [%s]", bankAccount.getId()), e, true);
        } finally {
            bankAccountsCache.remove(customer.stripeCustomerId);
        }
    }

    public boolean verifyFirstBankAccountForCustomer(final StripeCustomer customer, final Integer firstDebit, final Integer secondDebit) throws StripeApiCallException {
        final BankAccount bankAccount = getFirstBankAccount(getCustomer(customer));
        if (bankAccount == null) {
            throw new IllegalStateException(format("StripeCustomer with ID %s doesn't have any bank accounts to verify", customer.id));
        }

        final StripeBankVerificationDTO request = new StripeBankVerificationDTO();
        request.amounts = new ArrayList<>();
        request.amounts.add(firstDebit);
        request.amounts.add(secondDebit);

        try {
            final ExternalAccount verifiedAccount = bankAccount.verify(request.toMap(), platformRequestOptions());
            bankAccountsCache.remove(customer.stripeCustomerId);
            return verifiedAccount instanceof BankAccount
                    && equalsIgnoreCase(((BankAccount) verifiedAccount).getStatus(), BANK_ACCOUNT_STATUS_VERIFIED);
        } catch (CardException e) {
            Logger.warn(format("Unable to verify bank account with ID [%s]. Amounts do not match", bankAccount.getId()));
            return false;
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to verify bank account with ID [%s]", bankAccount.getId()), e);
        }
    }

    public ExportedPayout createPayout(final StripeAccount merchant, final Long amount, final String currency) throws StripeApiCallException {
        final Account account = getAccount(merchant);
        if (isNotTrue(account.getPayoutsEnabled())) {
            Logger.warn(format("Payouts are disabled for Stripe account with id %s and DIEMlife email %s", account.getId(), merchant.user.getEmail()));
            final Account.Requirements accountRequirements = account.getRequirements();
            if (equalsIgnoreCase(accountRequirements.getDisabledReason(), ACCOUNT_VERIFICATION_FIELDS_NEEDED)) {
                return ExportedPayout.from(accountRequirements);
            }
        }
        final BankAccount bankAccount = getFirstBankAccount(account);

        final Balance balance = getBalance(merchant);

        final ExportedPayout exportedPayout = new ExportedPayout();

        balance.getAvailable().stream()
                .filter(money -> equalsIgnoreCase(money.getCurrency(), currency))
                .forEach(money -> {
                    if (amount > money.getAmount()) {
                        throw new IllegalArgumentException("Requested amount is greater than total balance for the currency " + currency);
                    }
                    final Long rest = PAYMENT_SOURCE_TYPES.stream()
                            .map(type -> readPaymentSourceAmount(money, type))
                            .reduce(Pair.of("", amount), (remaining, current) -> {
                                final String sourceType = current.getKey();
                                final Long paymentSourceAmount = current.getValue();
                                final Long remainingAmount = remaining.getValue();
                                if (remainingAmount <= 0L) {
                                    return Pair.of(sourceType, 0L);
                                }
                                if (paymentSourceAmount > 0) {
                                    final StripePayoutDTO request = new StripePayoutDTO();
                                    final Long toWithdraw = remainingAmount > paymentSourceAmount ? paymentSourceAmount : remainingAmount;
                                    request.amount = toWithdraw;
                                    request.currency = lowerCase(currency);
                                    request.method = PAYOUT_METHOD_STANDARD;
                                    request.sourceType = new SnakeCaseStrategy().translate(sourceType);
                                    request.statementDescriptor = "DIEMLIFE PAYOUT";
                                    request.destination = bankAccount.getId();
                                    exportedPayout.add(createPayout(request, merchant));

                                    return Pair.of(sourceType, remainingAmount - toWithdraw);
                                } else {
                                    return Pair.of(sourceType, remainingAmount);
                                }
                            }).getValue();

                    Logger.debug(format("Requested payout amount : %s, rest : %s", amount, rest));
                });

        return exportedPayout;
    }

    public Optional<String> doesAccountNeedFields(final StripeAccount merchant) throws StripeApiCallException {
        if (merchant.stripeAccountId == null) {
            return Optional.empty();
        }
        final Account account = getAccount(merchant);
        if (isTrue(account.getChargesEnabled())) {
            final Account.Requirements accountRequirements = account.getRequirements();
            if (equalsIgnoreCase(accountRequirements.getDisabledReason(), ACCOUNT_VERIFICATION_FIELDS_NEEDED)) {
                return Optional.of(accountRequirements.getDisabledReason());
            }
        }
        return Optional.empty();
    }

    private Payout createPayout(final StripePayoutDTO request, final StripeAccount merchant) throws StripeApiCallException {
        try {
            return Payout.create(request.toMap(), connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to create payout Stripe Account with ID [%s]", merchant.stripeAccountId), e);
        }
    }

    private static Pair<String, Long> readPaymentSourceAmount(final Money money, final String paymentSource) {
        try {
            final Object result = BeanUtils
                    .getPropertyDescriptor(SourceTypes.class, paymentSource)
                    .getReadMethod()
                    .invoke(money.getSourceTypes());
            return result instanceof Long ? Pair.of(paymentSource, (Long) result) : Pair.of(paymentSource, 0L);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    private PaymentSourceCollection getCustomerBankAccounts(final Customer customer) throws StripeApiCallException {
        final PaymentSourcesRequestDTO request = new PaymentSourcesRequestDTO();
        request.object = PAYMENT_SOURCE_OBJECT_BANK_ACCOUNT;
        request.limit = 1;
        return Optional.ofNullable(customer.getSources()).map(sources -> {
            try {
                return sources.list(request.toMap(), platformRequestOptions());
            } catch (final StripeException e) {
                throw new StripeApiCallException(format("Unable to retrieve bank account of a Stripe Customer with ID [%s]", customer.getId()), e);
            }
        }).orElse(new PaymentSourceCollection());
    }

    private ExternalAccountCollection getConnectedAccountBankAccounts(final Account account) throws StripeApiCallException {
        final PaymentSourcesRequestDTO request = new PaymentSourcesRequestDTO();
        request.object = PAYMENT_SOURCE_OBJECT_BANK_ACCOUNT;
        request.limit = 1;
        try {
            return account.getExternalAccounts().list(request.toMap(), platformRequestOptions());
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to retrieve bank account of a Stripe Account with ID [%s]", account.getId()), e);
        }
    }

    private void validateStripeCustomers(final StripeCustomer merchant, final StripeCustomer buyer) {
        validateStripeMerchant(merchant);
        validateStripeBuyer(buyer);
    }

    private void validateStripeMerchant(final StripeCustomer merchant) {
        if (merchant == null) {
            throw new IllegalArgumentException("Merchant is required to place an order");
        }
    }

    private void validateStripeBuyer(final StripeCustomer buyer) {
        if (buyer == null) {
            throw new IllegalArgumentException("Buyer is required to place an order");
        }
    }

    private void validateCurrency(final String currency) {
        if (isBlank(currency)) {
            throw new IllegalArgumentException("Currency is required to send a payment");
        }
    }
    
    private StripeTokenType getTokenType(StripeOrderDTO order) {
        StripeTokenType result = StripeTokenType.UNKNOWN;
        
        if (order != null) {
            for (StripeOrderItemDTO item : order.items) {
                // just check the first one
                result = StripeTokenType.tokenToType(item.parent);
                Logger.debug("getTokenType - token " + item.parent + " is type " + result);
                break;
            }
        }
        
        return result;
    }
    
    private String registerFreeOrder(final StripeAccount merchant,
                                     final String billingEmail,
                                     final StripeShippingDTO billingInfo,
                                     final PurchaseOrder purchaseOrder,
                                     long questId,
                                     long eventId) throws StripeApiCallException {
        StripeOrderDTO newOrder = createOrder(billingEmail, billingInfo, purchaseOrder);
        StripeTokenType tokenType = getTokenType(newOrder);
        
        if (tokenType == StripeTokenType.SKU) {
            // Only do this if needed
            final Order placedOrder = placeOrder(merchant, billingEmail, newOrder);
            final Order freeOrder = markOrderFree(placedOrder, merchant, ((purchaseOrder.coupon == null) ? null : purchaseOrder.coupon.code));
            Logger.info("registerFreeOrder - setting free sku order with ID " + freeOrder.getId());
        } else {
            // TODO: do we have to do a charge for a free item?
            Logger.warn("registerFreeOrder - do we need to anything with a free order, if using Price API? ... " + newOrder.toJson());
        }

        String chargeId = "FREE";

        // Save the order to the DB
        // TODO: this code should be pulled out of here later.  Stripe needs to be refactored to expose some of the internal logic to outside so we can save
        // order data.  For now it was easier to pass down questId and eventId.
        String orderJson = newOrder.toJson();
        Logger.debug("registerFreeOrder - order: " + orderJson);
        StripeOrderDAO soDao = new StripeOrderDAO(this.config);
        soDao.insert(billingEmail, System.currentTimeMillis(), questId, eventId, /* stripeProductId */ null, merchant.stripeAccountId, /* buyer.stripeCustomerId */ null, 
            chargeId, orderJson, null);
            
        return chargeId;
    }

    private String payOrderWithSourceToken(final StripeAccount merchant,
                                           final StripeCustomer buyer,
                                           final String billingEmail,
                                           final StripeShippingDTO billingInfo,
                                           final PurchaseOrder purchaseOrder,
                                           final TransactionBreakdown breakdown,
                                           long questId,
                                           long eventId) throws StripeApiCallException {
        StripeOrderDTO newOrder = createOrder(billingEmail, billingInfo, purchaseOrder);
        StripeTokenType tokenType = getTokenType(newOrder);
        
        final Charge orderCharge;
        if (tokenType == StripeTokenType.SKU) {
            // Only do this if needed
            final Order placedOrder = placeOrder(merchant, billingEmail, newOrder);
            orderCharge = createChargeRelatedToOrder(merchant, buyer, newOrder, purchaseOrder, breakdown, questId);
            final Order paidOrder = markOrderPaid(placedOrder, merchant, orderCharge, ((purchaseOrder.coupon == null) ? null : purchaseOrder.coupon.code));
            
            Logger.debug("payOrderWithSourceToken - using sku for order by: " + billingEmail);
        } else {
            // Assume it is StripeTokenType.PRICE
            orderCharge = createChargeRelatedToOrder(merchant, buyer, newOrder, purchaseOrder, breakdown, questId);
            
            Logger.debug("payOrderWithSourceToken - using price for order by: " + billingEmail);
        }
        
        final String chargeStatus = orderCharge.getStatus();
        if (TRANSACTION_STATUS_FAILED.equalsIgnoreCase(chargeStatus)) {
            throw new IllegalStateException(format("Unexpected charge status %s of order with ID %s", chargeStatus, /* paidOrder.getId() */ newOrder.email));
        }
        
        String chargeId = orderCharge.getId();
        
        // Save the order to the DB
        // TODO: this code should be pulled out of here later.  Stripe needs to be refactored to expose some of the internal logic to outside so we can save
        // order data.  For now it was easier to pass down questId and eventId.
        String orderJson = newOrder.toJson();
        Logger.debug("payOrderWithSourceToken - order: " + orderJson);
        StripeOrderDAO soDao = new StripeOrderDAO(this.config);
        soDao.insert(billingEmail, System.currentTimeMillis(), questId, eventId, /* stripeProductId */ null, merchant.stripeAccountId, buyer.stripeCustomerId, chargeId, 
            orderJson, breakdown.toJson());
        
        Logger.info(format("Successfully created order Charge with ID [%s] and beneficiary ID [%s]", chargeId, merchant.stripeAccountId));

        return chargeId;
    }

    private StripeOrderDTO createOrder(final String billingEmail, final StripeShippingDTO billingInfo, final PurchaseOrder purchaseOrder) {
        final StripeOrderDTO order = new StripeOrderDTO();
        order.currency = lowerCase(purchaseOrder.currency);
        order.email = billingEmail;
        order.items = purchaseOrder.getOrderItemQuantities().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> {
                    final StripeOrderItemDTO orderItem = new StripeOrderItemDTO();
                    orderItem.parent = entry.getKey();
                    orderItem.quantity = entry.getValue();
                    orderItem.type = ORDER_ITEM_TYPE_SKU;
                    return orderItem;
                }).toArray(StripeOrderItemDTO[]::new);
        order.shipping = billingInfo;
        
        return order;
    }
    
    private Order placeOrder(final StripeAccount merchant,
                             final String billingEmail,
                             StripeOrderDTO placeOrder) throws StripeApiCallException {
        try {
            return Order.create(placeOrder.toMap(), connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to create order for user [%s]", billingEmail), e);
        }
    }

    private Charge createChargeRelatedToOrder(final StripeAccount merchant,
                                              final StripeCustomer customer,
                                              final StripeOrderDTO order,
                                              final PurchaseOrder purchaseOrder,
                                              final TransactionBreakdown breakdown,
                                              long questId) throws StripeApiCallException {

        final ChargeCreationDTO charge = new ChargeCreationDTO();
        charge.amount = breakdown.brutTotal;
        charge.currency = order.currency;
        charge.customer = customer == null || PaymentMode.CreditCardToken.equals(purchaseOrder.paymentMethod.paymentMode)
                ? null
                : customer.stripeCustomerId;
        charge.source = getPaymentSource(customer, purchaseOrder.paymentMethod);
        
        charge.description = "Payment for order by " + /* order.getId(); */ order.email;
        // TODO: I suspect this is not needed at all and our code to save order data ourselves can supersede it.  Remove this later if fine.
        //charge.metadata.put("orderId", order.getId());
        
        if (purchaseOrder.coupon != null && isTrue(purchaseOrder.coupon.valid)) {
            charge.metadata.put("couponCode", purchaseOrder.coupon.code);
        }

        charge.statementDescriptor = upperCase("DIEMLIFE ORDER");
        charge.statementDescriptorSuffix = upperCase("DIEMLIFE ORDER");
        final Quests quest = QuestsDAO.findById((int) questId);
        if (quest != null) {
            final User user = UserService.getById(quest.getCreatedBy());
            if (user != null && user.getUserName() != null) {
                charge.statementDescriptor = upperCase(user.getUserName());
                charge.statementDescriptorSuffix = upperCase(user.getUserName());
            }
        }

        charge.transferGroup = purchaseOrder.getTransferGroupKey();
        if ((breakdown.netTotal - breakdown.discount) > 0) {
            charge.transferData = new StripeChargeDestinationDTO();
            charge.transferData.destination = merchant.stripeAccountId;
            charge.transferData.amount = breakdown.netTotal - breakdown.discount;
        }

        try {
            return Charge.create(charge.toMap(), platformRequestOptions());
        } catch (final StripeException e) {
            //throw new StripeApiCallException(format("Unable to create charge related to order with ID [%s]", order.getId()), e);
            throw new StripeApiCallException(format("Unable to create charge related to order by [%s] due to code [%s]", order.email, e.getCode()), e);
        }
    }

    private Charge createChargeForQuestBacking(final StripeAccount beneficiary,
                                               final @Nullable StripeCustomer customer,
                                               final PaymentMethod paymentMethod,
                                               final String currency,
                                               final Long amount,
                                               final Double formFee,
                                               final long questId) throws StripeApiCallException {
        final TransactionBreakdown breakdown = transactionBreakdown(
                amount,
                paymentMethod.paymentMode,
                beneficiary.user.isAbsorbFees(),
                false,
                beneficiary.user,
                formFee
        );
        final ChargeCreationDTO charge = new ChargeCreationDTO();
        charge.amount = breakdown.brutTotal;
        charge.currency = currency;
        charge.customer = customer == null || PaymentMode.CreditCardToken.equals(paymentMethod.paymentMode)
                ? null
                : customer.stripeCustomerId;
        charge.source = getPaymentSource(customer, paymentMethod);
        charge.description = "Backing " + beneficiary.user.getEmail() + "'s Quest";

        charge.statementDescriptor = upperCase(DESC_DIEM_LIFE_BACKING);
        charge.statementDescriptorSuffix = upperCase(DESC_DIEM_LIFE_BACKING);
        final Quests quest = QuestsDAO.findById((int) questId);
        if (quest != null) {
            final User user = UserService.getById(quest.getCreatedBy());
            if (user != null && user.getUserName() != null) {
                charge.statementDescriptor = upperCase(user.getUserName());
                charge.statementDescriptorSuffix = upperCase(user.getUserName());
            }
        }

        if ((breakdown.netTotal - breakdown.discount) > 0) {
            charge.transferData = new StripeChargeDestinationDTO();
            charge.transferData.destination = beneficiary.stripeAccountId;
            charge.transferData.amount = breakdown.netTotal - breakdown.discount;
        }

        try {
            return Charge.create(charge.toMap(), platformRequestOptions());
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to create charge for backing Quest of user [%s]", beneficiary.user.getEmail()), e);
        }
    }

    private Charge createChargeForQuestFundraising(final @NotNull StripeAccount beneficiary,
                                                   final @Nullable StripeCustomer customer,
                                                   final @NotNull FundraisingLink fundraisingLink,
                                                   final @NotNull PaymentMethod paymentMethod,
                                                   final @NotNull String currency,
                                                   final @NotNull Long amount,
                                                   final Double formFee,
                                                   final long questId) throws StripeApiCallException {
        final TransactionBreakdown breakdown = transactionBreakdown(
                amount,
                paymentMethod.paymentMode,
                beneficiary.user.isAbsorbFees(),
                false,
                beneficiary.user,
                formFee
        );
        final ChargeCreationDTO charge = new ChargeCreationDTO();
        charge.amount = breakdown.brutTotal;
        charge.currency = currency;
        charge.customer = customer == null || PaymentMode.CreditCardToken.equals(paymentMethod.paymentMode)
                ? null
                : customer.stripeCustomerId;
        charge.source = getPaymentSource(customer, paymentMethod);
        charge.description = "Fundraising for " + beneficiary.user.getEmail() + "'s Quest '" + fundraisingLink.quest.getTitle() + "'";
        charge.statementDescriptor = upperCase(DESC_DIEM_LIFE_FUNDRAISING);
        charge.statementDescriptorSuffix = upperCase(DESC_DIEM_LIFE_FUNDRAISING);

        final Quests quest = QuestsDAO.findById((int) questId);
        if (quest != null) {
            final User user = UserService.getById(quest.getCreatedBy());
            if (user != null && user.getUserName() != null) {
                charge.statementDescriptor = upperCase(user.getUserName());
                charge.statementDescriptorSuffix = upperCase(user.getUserName());
            }
        }

        charge.transferGroup = format(CHARGE_GROUP_FUNDRAISING_TEMPLATE, fundraisingLink.fundraiser.getId(), fundraisingLink.quest.getId());
        if ((breakdown.netTotal - breakdown.discount) > 0) {
            charge.transferData = new StripeChargeDestinationDTO();
            charge.transferData.destination = beneficiary.stripeAccountId;
            charge.transferData.amount = breakdown.netTotal - breakdown.discount;
        }

        try {
            return Charge.create(charge.toMap(), platformRequestOptions());
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to create charge for Quest fundraising for user [%s]", beneficiary.user.getEmail()), e);
        }
    }

    private Order markOrderPaid(final Order order, final StripeAccount merchant, final Charge charge, final String couponCode) throws StripeApiCallException {
        if (TRANSACTION_STATUS_FAILED.equalsIgnoreCase(charge.getStatus())) {
            throw new IllegalStateException("Charge was rejected for order with ID " + order.getId());
        }

        final OrderUpdateRequestDTO updateRequest = new OrderUpdateRequestDTO();

        updateRequest.metadata.put("chargeId", charge.getId());
        if (isNotBlank(couponCode)) {
            updateRequest.metadata.put("couponCode", couponCode);
        }

        try {
            return order.update(updateRequest.toMap(), connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to mark order with ID [%s] as paid", order.getId()), e);
        }
    }

    private Order markOrderFree(final Order order, final StripeAccount merchant, final String couponCode) throws StripeApiCallException {
        final OrderUpdateRequestDTO updateRequest = new OrderUpdateRequestDTO();

        updateRequest.metadata.put("free", Boolean.TRUE);
        if (isNotBlank(couponCode)) {
            updateRequest.metadata.put("couponCode", couponCode);
        }

        try {
            return order.update(updateRequest.toMap(), connectAccountRequestOptions(merchant));
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to mark order with ID [%s] as free", order.getId()), e);
        }
    }

    private RequestOptions platformRequestOptions() {
        return RequestOptions.builder()
                .setApiKey(stripeApiKey)
                .build();
    }

    private RequestOptions connectAccountRequestOptions(final StripeAccount merchant) {
        return RequestOptions.builder()
                .setApiKey(stripeApiKey)
                .setStripeAccount(merchant.stripeAccountId)
                .build();
    }

    public void updateConnectAccount(final User user,
                                     final StripeAccount stripeEntity,
                                     final PersonalInfoDTO personalInfo) throws StripeApiCallException {
        final Account account = getAccount(stripeEntity);
        final StripeAccountUpdateDTO request = new StripeAccountUpdateDTO();

        request.email = user.getEmail();
        request.individual = new StripeIndividualDTO();
        request.individual.firstName = user.getFirstName();
        request.individual.lastName = user.getLastName();
        request.individual.email = user.getEmail();
        request.individual.address = new StripeAddressDTO();
        request.individual.address.postalCode = user.getZip();

        if (personalInfo != null) {
            if (hasText(personalInfo.getDob())) {
                final DateTime dateTime = new DateTime(personalInfo.getDob(), DateTimeZone.UTC);
                final DateOfBirthDTO dateOfBirth = new DateOfBirthDTO();
                dateOfBirth.day = dateTime.getDayOfMonth();
                dateOfBirth.month = dateTime.getMonthOfYear();
                dateOfBirth.year = dateTime.getYear();
                request.individual.dob = dateOfBirth;
            }
            if (hasText(personalInfo.getLast4())) {
                request.individual.ssnLast4 = personalInfo.getLast4();
            }
            if (hasText(personalInfo.getAddress())) {
                request.individual.address.line1 = personalInfo.getAddress();
            }
            if (hasText(personalInfo.getPersonalId())) {
                request.individual.idNumber = personalInfo.getPersonalId();
            }
            if (hasText(personalInfo.getPhone())) {
                request.individual.phone = personalInfo.getPhone();
            }
            if (hasText(personalInfo.getUrl())) {
                request.businessProfile = new BusinessProfileDTO();
                request.businessProfile.url = personalInfo.getUrl();
            }
        }

        try {
            account.update(request.toMap(), platformRequestOptions());
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to update Stripe Account with ID [%s]", stripeEntity.stripeAccountId), e, true);
        }
    }

    public void updateCustomer(final User user, final StripeCustomer stripeEntity) {
        final Customer customer = getCustomer(stripeEntity);
        final StripeCustomerDTO request = new StripeCustomerDTO();
        request.email = user.getEmail();
        try {
            customer.update(request.toMap(), platformRequestOptions());
        } catch (final StripeException e) {
            throw new StripeApiCallException(format("Unable to update Stripe Customer with ID [%s]", stripeEntity.stripeCustomerId), e, true);
        }
    }

    private static class PaymentSourcesRequestDTO extends StripeDTO {
        public String object;
        public int limit;
    }

    private static class PaymentSourceDTO extends StripeDTO {
        public String source;
    }

    private static class BankAccountCreationDTO extends StripeDTO {
        public String externalAccount;
    }

    private static abstract class PaymentTokenDTO extends StripeDTO {
        public String customer;
    }

    private static class CreditCardPaymentTokenDTO extends PaymentTokenDTO {
        public String card;
    }

    private static class BankAccountPaymentTokenDTO extends PaymentTokenDTO {
        public String bankAccount;
    }

    private static class OrderUpdateRequestDTO extends StripeDTO {
    }

    public static class ExportedCreditCardData implements Serializable {
        public String cardType;
        public String lastFourDigits;
        public Long expiryMonth;
        public Long expiryYear;

        public static ExportedCreditCardData from(final Card stripeCard) {
            if (stripeCard == null) {
                return null;
            }
            final ExportedCreditCardData data = new ExportedCreditCardData();
            data.cardType = stripeCard.getBrand();
            data.lastFourDigits = stripeCard.getLast4();
            data.expiryMonth = stripeCard.getExpMonth();
            data.expiryYear = stripeCard.getExpYear();
            return data;
        }
    }

    public static class ExportedBankAccountData implements Serializable {
        public String id;
        public String name;
        public String lastFourDigits;
        public boolean verified;
        public boolean customer;

        public static ExportedBankAccountData from(final BankAccount stripeBankAccount) {
            if (stripeBankAccount == null) {
                return null;
            }
            final ExportedBankAccountData data = new ExportedBankAccountData();
            data.id = stripeBankAccount.getLast4();
            data.name = stripeBankAccount.getBankName();
            data.lastFourDigits = stripeBankAccount.getLast4();
            data.customer = isNotBlank(stripeBankAccount.getCustomer());
            data.verified = equalsIgnoreCase(stripeBankAccount.getStatus(), BANK_ACCOUNT_STATUS_VERIFIED);
            return data;
        }
    }

    public static class ExportedProduct implements Serializable {
        
        public String id;
        public String name;
        public String description;
        public boolean active;
        public ExportedProductVariant[] variants;
        public Integer questId;
        public String registrationTemplate;
        public boolean showDiscounts;
        public Date eventDate;
        
        public static ExportedProduct fromSkus(final Product product, final FeeUtility feeUtility, final SkuCollection skus) {
            if (product == null) {
                return null;
            }
            final ExportedProduct data = toExportedProduct(product);
            
            data.variants = skus == null || skus.getData() == null ? new ExportedProductVariant[]{} : skus.getData()
                    .stream()
                    .map(sku -> ExportedProductVariant.fromSku(sku).withTransactionBreakdown(feeUtility))
                    .toArray(ExportedProductVariant[]::new);

            return data;
        }

        public static ExportedProduct fromPrices(final Product product, final FeeUtility feeUtility, final PriceCollection skus) {
            if (product == null) {
                return null;
            }
            final ExportedProduct data = toExportedProduct(product);
            
            data.variants = skus == null || skus.getData() == null ? new ExportedProductVariant[]{} : skus.getData()
                    .stream()
                    .map(sku -> ExportedProductVariant.fromPrice(sku).withTransactionBreakdown(feeUtility))
                    .toArray(ExportedProductVariant[]::new);

            return data;
        }

        protected static ExportedProduct toExportedProduct(Product product) {
            final ExportedProduct data = new ExportedProduct();
            data.id = product.getId();
            data.name = product.getName();
            data.description = product.getDescription();
            data.active = isTrue(product.getActive());
            return data;
        }
    }

    public static class ExportedProductVariant implements Serializable {
        public String id;
        public Long price;
        public Integer platformFee;
        public String currency;
        public boolean active;
        public Map<String, String> attributes;

        public static ExportedProductVariant fromSku(final Sku sku) {
            if (sku == null) {
                return null;
            }
            final ExportedProductVariant data = new ExportedProductVariant();
            data.id = sku.getId();
            data.price = sku.getPrice();
            data.currency = sku.getCurrency();
            data.active = isTrue(sku.getActive());
            data.attributes = sku.getAttributes();
            return data;
        }

        public static ExportedProductVariant fromPrice(final Price price) {
            if (price == null) {
                return null;
            }
            final ExportedProductVariant data = new ExportedProductVariant();
            data.id = price.getId();
            data.price = price.getUnitAmount();
            data.currency = price.getCurrency();
            data.active = isTrue(price.getActive());
            data.attributes = price.getMetadata();
            return data;
        }

        private ExportedProductVariant withTransactionBreakdown(final FeeUtility feeUtility) {
            final TransactionBreakdown breakdown = feeUtility.calculateTicketsFees(
                    skuId -> this,
                    couponCode -> null,
                    ImmutableMap.of(this.id, 1),
                    null,
                    null
            );
            this.platformFee = Long.valueOf(breakdown.platformFee).intValue();
            return this;
        }
    }

    public static class ExportedCoupon implements Serializable {
        public String code;
        public boolean valid;
        public boolean free;
        public BigDecimal percentOff;
        public Long amountOff;

        public static ExportedCoupon from(final Coupon coupon) {
            if (coupon == null) {
                return null;
            }
            final ExportedCoupon data = new ExportedCoupon();
            data.code = coupon.getId();
            data.valid = isTrue(coupon.getValid());
            data.free = coupon.getPercentOff() != null && coupon.getPercentOff().compareTo(BigDecimal.valueOf(99)) > 0;
            data.percentOff = coupon.getPercentOff() == null ? BigDecimal.ZERO : coupon.getPercentOff()
                    .setScale(2, RoundingMode.DOWN)
                    .divide(BigDecimal.valueOf(100), RoundingMode.FLOOR);
            data.amountOff = coupon.getAmountOff() == null ? 0L : coupon.getAmountOff();
            return data;
        }
    }

    public static class ExportedPayout implements Serializable {
        public final List<ExportedPayoutData> payouts = new ArrayList<>();
        public ExportedPayoutVerification verification;

        public static class ExportedPayoutData implements Serializable {
            public String id;
            public String status;
            public ExportedPayoutFailure failure;

            public static ExportedPayoutData from(final Payout payout) {
                if (payout == null) {
                    return null;
                }
                final ExportedPayoutData data = new ExportedPayoutData();
                data.id = trimToNull(payout.getId());
                data.status = trimToNull(payout.getStatus());
                data.failure = ExportedPayoutFailure.from(payout);
                return data;
            }
        }

        public static ExportedPayout from(final Account.Requirements requirements) {
            if (requirements == null) {
                return null;
            }
            final ExportedPayout data = new ExportedPayout();
            data.verification = ExportedPayoutVerification.from(requirements);
            return data;
        }

        public static class ExportedPayoutFailure implements Serializable {
            public String code;
            public String message;

            private static ExportedPayoutFailure from(final Payout payout) {
                if (isBlank(payout.getFailureCode()) && isBlank(payout.getFailureMessage())) {
                    return null;
                }
                final ExportedPayoutFailure data = new ExportedPayoutFailure();
                data.code = trimToNull(payout.getFailureCode());
                data.message = trimToNull(payout.getFailureMessage());
                return data;
            }
        }

        public void add(final Payout payout) {
            if (payout == null) {
                return;
            }
            payouts.add(ExportedPayoutData.from(payout));
        }

        public static class ExportedPayoutVerification implements Serializable {
            public String reason;
            public List<String> missingFields = new ArrayList<>();

            private static ExportedPayoutVerification from(final Account.Requirements requirements) {
                final ExportedPayoutVerification data = new ExportedPayoutVerification();
                data.reason = requirements.getDisabledReason();
                return data;
            }
        }
    }

    public static class ExportedBalance implements Serializable {
        public long available;
        public long pending;
    }

    public static final class NullBankAccount extends BankAccount {
        public static final NullBankAccount INSTANCE = new NullBankAccount();

        private NullBankAccount() {
            super();
        }
    }

    public static class PaymentMethod implements Serializable {
        public final PaymentMode paymentMode;
        public final String lastFour;
        public final String token;
        public final boolean save;

        public PaymentMethod(final PaymentMode paymentMode, final String lastFour, final String token, final boolean save) {
            switch (paymentMode) {
                case BankAccount:
                    this.paymentMode = PaymentMode.BankAccount;
                    this.lastFour = null;
                    this.token = null;
                    this.save = false;
                    break;
                case CreditCard:
                    this.paymentMode = PaymentMode.CreditCard;
                    this.lastFour = lastFour;
                    this.token = null;
                    this.save = false;
                    break;
                case CreditCardToken:
                    this.paymentMode = PaymentMode.CreditCardToken;
                    this.lastFour = null;
                    this.token = token;
                    this.save = save;
                    break;
                default:
                    throw new IllegalArgumentException(format("Unknown payment mode : %s", paymentMode));
            }
        }

    }

    public static class PurchaseOrder implements Serializable {
        public Map<String, Pair<ExportedProductVariant, Integer>> orderItems;
        public String currency;
        public PaymentMethod paymentMethod;
        public ExportedCoupon coupon;
        public ExportedProduct product;

        public String getTransferGroupKey() {
            return null;
        }

        public String getCouponUsed() {
            return Optional.ofNullable(this.coupon)
                    .filter(coupon -> coupon.valid)
                    .map(coupon -> coupon.code)
                    .orElse(null);
        }

        public Map<String, Integer> getOrderItemQuantities() {
            return this.orderItems.entrySet()
                    .stream()
                    .collect(toMap(Map.Entry::getKey, entry -> Optional.ofNullable(entry.getValue()).map(Pair::getRight).orElse(0)));
        }

        public PaymentMode getPaymentMode() {
            return Optional.ofNullable(this.paymentMethod).map(method -> method.paymentMode).orElse(null);
        }
    }

    public static class TicketsPurchaseOrder extends PurchaseOrder {
        public Happening happening;

        @Override
        public String getTransferGroupKey() {
            return String.format(CHARGE_GROUP_EVENT_TEMPLATE, happening.quest.getId());
        }
    }

    public static enum StripeTokenType {
        UNKNOWN,
        SKU,
        PRICE,
        PRODUCT;
        
        // Stripe id tokens
        //
        // These are all of the form: [type]_[some token]
        private static final String TYPE_SKU = "sku";
        private static final String TYPE_PRICE = "price";
        private static final String TYPE_PRODUCT = "prod";
        // TODO: add others like py_, req_ later if needed
        
        public static StripeTokenType tokenToType(String token) {
            StripeTokenType result = UNKNOWN;
            
            int index;
            if ((index = token.indexOf('_')) != -1) {
                // Extract token prefix and match it
                switch (token.substring(0, index)) {
                    case TYPE_SKU:
                        result = SKU;
                        break;
                    case TYPE_PRICE:
                        result = PRICE;
                        break;
                    case TYPE_PRODUCT:
                        result = PRODUCT;
                        break;
                    default:
                        // unknown
                        break;
                }
            }
            // else Invalid token
            
            return result;
        }
    }
}
