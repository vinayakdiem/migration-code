package com.diemlife.services;

import com.diemlife.dao.PersonalInfoDAO;
import com.diemlife.dto.PaymentPersonalInfoDTO;
import com.diemlife.dto.TransactionExportDTO;
import com.diemlife.models.User;
import play.db.jpa.JPAApi;
import com.diemlife.utils.TransactionResponse.ExportTransactionResponse;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * Transaction service
 * Created 03/11/2020
 *
 * @author SYushchenko
 */
@Service
public class TransactionsService {

	@Autowired
    private PaymentTransactionFacade paymentTransactionFacade;

    /**
     * Find all transactions for a user
     *
     * @param user {@link User}
     * @return Collection {@link TransactionExportDTO}
     */
    public List<TransactionExportDTO> getAllTransactionsExport(final User user, final Optional<Integer> questId) {
        final List<ExportTransactionResponse> exportTransactionResponses = getExportTransactionResponse(user, questId);
        final Map<Long, PaymentPersonalInfoDTO> paymentsPersonalInfo =
                getPaymentPersonalInformations(getAllTransactionsIdByTransactionResponse(exportTransactionResponses));

        return exportTransactionResponses.stream()
                .peek(l -> {
                    PaymentPersonalInfoDTO paymentPersonalInfoDTO = paymentsPersonalInfo.get(l.id);
                    if (paymentPersonalInfoDTO != null) {
                        l.from.firstName = paymentPersonalInfoDTO.getFirstName();
                        l.from.lastName = paymentPersonalInfoDTO.getLastName();
                        l.from.email = paymentPersonalInfoDTO.getEmail();
                    }
                })
                .map(TransactionExportDTO::from)
                .collect(toList());
    }

    private List<Long> getAllTransactionsIdByTransactionResponse(final List<ExportTransactionResponse> exportTransactionResponses) {
        return exportTransactionResponses.stream()
                .map(l -> l.id)
                .collect(toList());
    }

    private List<ExportTransactionResponse> getExportTransactionResponse(final User user, final Optional<Integer> questId) {
        return paymentTransactionFacade.listTransactions(user, true, true, questId)
                .stream()
                .filter(element -> element instanceof ExportTransactionResponse)
                .map(ExportTransactionResponse.class::cast)
                .collect(toList());
    }

    private Map<Long, PaymentPersonalInfoDTO> getPaymentPersonalInformations(final List<Long> transaction) {
        return new PersonalInfoDAO().getPersonalInfoByPaymentTransactions(transaction);
    }
}
