package com.diemlife.dao;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Repository;

import com.diemlife.dto.PaymentPersonalInfoDTO;
import com.diemlife.models.PersonalInfo;

/**
 * DAO for {@link PersonalInfo}
 * Created 2/11/2020
 *
 * @author SYushchenko
 */
@Repository
public class PersonalInfoDAO extends TypedDAO<PersonalInfo> {

	
	 @PersistenceContext
	  private EntityManager entityManager;
    /**
     * Search for personal information by payment transaction Id
     *
     * @param transactions list of payment transaction IDs
     * @return Map from payment transaction Id and {@link PaymentPersonalInfoDTO}
     */
    public Map<Long, PaymentPersonalInfoDTO> getPersonalInfoByPaymentTransactions(final List<Long> transactions) {
        try {
            TypedQuery<PaymentPersonalInfoDTO> query = entityManager.createQuery(
                    " SELECT NEW dto.PaymentPersonalInfoDTO(qb.paymentTransaction.id, pi.firstName, " +
                            " pi.lastName, pi.email) " +
                            " FROM PersonalInfo pi " +
                            " INNER JOIN QuestBackings qb ON qb.billingPersonalInfo.id = pi.id " +
                            " WHERE qb.paymentTransaction.id IN (:paymentTransactionId)", PaymentPersonalInfoDTO.class);
            query.setParameter("paymentTransactionId", transactions);

            return query.getResultList()
                    .stream()
                    .collect(Collectors.toMap(PaymentPersonalInfoDTO::getPaymentTransactionId, o -> o));

        } catch (final PersistenceException | IllegalStateException e) {
            return Collections.emptyMap();
        }
    }

}
