package com.diemlife.services;

import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.typesafe.config.Config;
import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.dao.PaymentTransactionDAO;
import com.diemlife.dao.QuestBackingDAO;
import com.diemlife.dao.QuestBackingLiteDAO;
import com.diemlife.dao.StripeCustomerDAO;
import com.diemlife.dto.FundraisingLinkDTO;
import com.diemlife.dto.FundraisingTotalDTO;
import com.diemlife.dto.SubscriptionListDTO;
import com.diemlife.exceptions.RequiredParameterMissingException;
import com.diemlife.models.PaymentTransaction;
import com.diemlife.models.RecurringQuestBackingTransaction;
import com.diemlife.models.StripeAccount;
import com.diemlife.models.User;
import com.diemlife.models.QuestBacking;
import play.db.jpa.JPAApi;
import com.diemlife.utils.TransactionResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static com.diemlife.utils.TransactionResponse.fromCharge;
import static com.diemlife.utils.TransactionResponse.fromFreeTicket;
import static com.diemlife.utils.TransactionResponse.fromPayout;

@Singleton
public class PaymentTransactionFacade {

    private final JPAApi jpaApi;
    private final Config configuration;
    private final StripeConnectService stripeService;

    @Inject
    public PaymentTransactionFacade(final JPAApi jpaApi,
                                    final Config configuration,
                                    final StripeConnectService stripeService) {
        this.jpaApi = jpaApi;
        this.configuration = configuration;
        this.stripeService = stripeService;
    }

    public List<TransactionResponse> listTransactions(final User user,
                                                      final boolean all,
                                                      final boolean export,
                                                      final EntityManager em,
                                                      final Optional<Integer> questId) {
        if (user == null) {
            throw new RequiredParameterMissingException("user");
        }
        final PaymentTransactionDAO dao = new PaymentTransactionDAO(em);
        final List<PaymentTransaction> transactions = questId.isPresent() ? dao.getTransactionsPerQuest(questId.get()) : dao.getLastTransactions(user.getId(), all);
        final Map<Long, QuestBacking> questBackingMap = getQuestBackingMapByTransactions(transactions);
        final String defaultCurrency = configuration.getString("application.currency");
        final List<TransactionResponse> result = new ArrayList<>();

        transactions.stream()
                .filter(transaction -> {
                    final String id = transaction.stripeTransactionId;
                    return startsWith(id, "ch_") ||
                            startsWith(id, "py_") ||
                            startsWith(id, "po_") ||
                            equalsIgnoreCase(id, "FREE");
                })
                .forEach(transaction -> {
                    QuestBacking questBacking = questBackingMap.get(transaction.id);
                    final String id = transaction.stripeTransactionId;
                    if (startsWith(id, "ch_") || startsWith(id, "py_")) {
                        try {
                            result.add(fromCharge(stripeService.retrieveChargeInformation(id, transaction.to, true), transaction, user, export, questBacking));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (startsWith(id, "po_")) {
                        result.add(fromPayout(stripeService.retrievePayoutInformation(id, transaction.to), transaction, user, export, questBacking));
                    }
                    if (equalsIgnoreCase(id, "FREE")) {
                        result.add(fromFreeTicket(defaultCurrency, transaction, user, export, questBacking));
                    }
                });

        return result.stream().filter(Objects::nonNull).collect(toList());
    }

    private Map<Long, QuestBacking> getQuestBackingMapByTransactions(final List<PaymentTransaction> transactions) {
        final List<Long> transactionsIds = transactions.stream().map(l -> l.id).collect(Collectors.toList());
        return new QuestBackingDAO(jpaApi.em()).getQuestBackingsByPaymentTransactions(transactionsIds)
                .stream()
                .collect(toMap(l -> l.getPaymentTransaction().id, o -> o));
    }

    public List<SubscriptionListDTO> listSubscriptionsForCustomer(final User buyer, final EntityManager em) {
        final PaymentTransactionDAO transactionDAO = new PaymentTransactionDAO(em);
        return transactionDAO.getSubscriptionsForCustomer(buyer.getId())
                .stream()
                .map(transaction -> {
                    final Subscription subscription = stripeService.getQuestBackingSubscription(transaction.stripeTransactionId, transaction.to);
                    if (subscription != null) {
                        final String envUrl = configuration.getString(DeploymentEnvironments.valueOf(configuration.getString("application.mode")).getBaseUrlKey());
                        return SubscriptionListDTO.toDTO(subscription)
                                .withQuest(transaction.quest)
                                .withDoer(transaction.to.user)
                                .build(envUrl);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public List<TransactionResponse> listTransactionsForSubscription(final String subscriptionId, final User buyer, final EntityManager em) {
        final PaymentTransactionDAO transactionDAO = new PaymentTransactionDAO(em);
        final PaymentTransaction subscriptionTransaction = getCustomerSubscriptionById(subscriptionId, buyer, transactionDAO);
        final StripeCustomerDAO stripeCustomerDAO = new StripeCustomerDAO(em);
        final List<TransactionResponse> result = new ArrayList<>();
        stripeService.getInvoicesForSubscription(subscriptionId, stripeCustomerDAO.getByUserId(subscriptionTransaction.to.user.getId(), StripeAccount.class)).forEach(charge ->
                {
                    try {
                        result.add(fromCharge(charge, subscriptionTransaction, buyer, false, null));
                    } catch (StripeException e) {
                        e.printStackTrace();
                    }
                }
        );
        return result.stream().filter(Objects::nonNull).collect(toList());
    }

    public void cancelSubscriptionForCustomer(final String subscriptionId, final User buyer, final EntityManager em) {
        final PaymentTransactionDAO transactionDAO = new PaymentTransactionDAO(em);
        final PaymentTransaction subscriptionTransaction = getCustomerSubscriptionById(subscriptionId, buyer, transactionDAO);
        if (subscriptionTransaction.valid) {
            final Subscription subscription = stripeService.cancelQuestBackingSubscription(subscriptionId, subscriptionTransaction.to);
            if (subscription != null) {
                subscriptionTransaction.valid = false;
                transactionDAO.save((RecurringQuestBackingTransaction) subscriptionTransaction, RecurringQuestBackingTransaction.class);
            }
        } else {
            throw new IllegalStateException(format("PaymentTransaction with ID [%s] and customer user ID [%s] is not valid", subscriptionId, buyer.getId()));
        }
    }

    public List<FundraisingTotalDTO> getQuestBackingFundraisingTotals(final FundraisingLinkDTO dto) {
        if (dto == null || dto.quest == null || dto.doer == null) {
            return emptyList();
        } else {
            return new QuestBackingLiteDAO(jpaApi.em()).getFundraisingTotals(dto.quest.id, dto.doer.id);
        }
    }

    private PaymentTransaction getCustomerSubscriptionById(final String subscriptionId, final User buyer, final PaymentTransactionDAO transactionDAO) {
        final PaymentTransaction subscriptionTransaction = transactionDAO.getSubscriptionsForCustomer(buyer.getId())
                .stream()
                .filter(transaction -> subscriptionId.equals(transaction.stripeTransactionId))
                .findFirst()
                .orElse(null);
        if (subscriptionTransaction != null) {
            return subscriptionTransaction;
        } else {
            throw new IllegalStateException(format("PaymentTransaction not found for ID [%s] and customer user ID [%s]", subscriptionId, buyer.getId()));
        }
    }

}