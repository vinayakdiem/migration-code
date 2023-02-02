package com.diemlife.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import com.diemlife.constants.QuestEdgeType;
import com.diemlife.dao.QuestEdgeDAO;
import com.diemlife.dao.QuestTeamDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dto.SubscriptionListDTO;
import com.diemlife.dto.TransactionExportDTO;
import com.diemlife.models.QuestEdge;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import play.Logger;
import play.db.Database;
import play.db.NamedDatabase;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import security.JwtSessionLogin;
import services.PaymentTransactionFacade;
import services.TransactionsService;
import services.UserProvider;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static com.diemlife.utils.CsvUtils.writeCsvToStream;

@JwtSessionLogin
public class TransactionsController extends Controller {

    private final JPAApi jpaApi;
    private final MessagesApi messagesApi;
    private final UserProvider userProvider;
    private final PaymentTransactionFacade paymentTransactionFacade;
    private final TransactionsService transactionsService;
    private Database dbRo;

    @Inject
    public TransactionsController(final JPAApi jpaApi,
                                  @NamedDatabase("ro") Database dbRo,
                                  final MessagesApi messagesApi,
                                  final UserProvider userProvider,
                                  final PaymentTransactionFacade paymentTransactionFacade,
                                  final TransactionsService transactionsService) {
        this.jpaApi = jpaApi;
        this.dbRo = dbRo;
        this.messagesApi = messagesApi;
        this.userProvider = userProvider;
        this.paymentTransactionFacade = paymentTransactionFacade;
        this.transactionsService = transactionsService;
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result downloadTransactions(final @NotNull String format, final Optional<Integer> questId) {
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized(TextNode.valueOf("Session user must not be null"));
        }

        Logger.info("downloading transactions for user [{}] and quest [{}]", user.getEmail(), questId.orElse(null));

//        check if quest ID is MQ or non
//        if MQ then run loop on the Parent Quests and get the transaction export
        List<QuestEdge> children = new LinkedList<QuestEdge>();
        List<TransactionExportDTO> transactionsExport = new LinkedList<TransactionExportDTO>();
        if (questId.isPresent()) {
            Long _questId = new Long(questId.get().intValue());
            try (Connection c = dbRo.getConnection()) {
                QuestEdgeDAO qeDao = QuestEdgeDAO.getInstance();
                children = qeDao.getEdgesByType(c, _questId, QuestEdgeType.CHILD.toString());
            } catch (Exception e) {
                Logger.error("buildQuestPageData - error with edges", e);
            }

            if (!children.isEmpty()) {
                for (QuestEdge edge : children) {
                    Integer _childDst = (int) edge.getQuestDst();
                    Optional<Integer> childQuestId = Optional.of(_childDst);
                    transactionsExport.addAll(transactionsService.getAllTransactionsExport(user, childQuestId));
                }
            } else {
                transactionsExport = transactionsService.getAllTransactionsExport(user, questId);
            }
        } else {
            transactionsExport = transactionsService.getAllTransactionsExport(user, questId);
        }

        final Messages messages = messagesApi.preferred(request());
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            writeCsvToStream(transactionsExport, output, UTF_8, messages::at);

            return ok(new String(output.toByteArray(), UTF_8), UTF_8.name())
                    .as("text/csv")
                    .withHeader(CONTENT_DISPOSITION, "attachment; filename=transactions.csv");
        } catch (final IOException e) {
            Logger.error(e.getMessage(), e);

            return internalServerError();
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result listSubscriptions() {
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized(TextNode.valueOf("Session user must not be null for listing subscription"));
        }
        final List<SubscriptionListDTO> subscriptions = paymentTransactionFacade.listSubscriptionsForCustomer(user, jpaApi.em());
        subscriptions.sort((Comparator.comparing(s -> s.nextDueOn)));
        final ArrayNode result = Json.newArray()
                .addAll(subscriptions.stream().map(Json::toJson).collect(toList())
        );
        return ok(result);
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result listTransactionsForSubscription(final @NotNull String subscriptionId) {
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized(TextNode.valueOf("Session user must not be null for listing subscription transactions"));
        }
        final ArrayNode result = Json.newArray().addAll(paymentTransactionFacade.listTransactionsForSubscription(subscriptionId, user, jpaApi.em())
                .stream().map(Json::toJson).collect(toList())
        );
        return ok(result);
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result cancelSubscription(final @NotNull String subscriptionId) {
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized(TextNode.valueOf("Session user must not be null for cancelling a subscription"));
        }

        paymentTransactionFacade.cancelSubscriptionForCustomer(subscriptionId, user, jpaApi.em());

        return ok();
    }
}
