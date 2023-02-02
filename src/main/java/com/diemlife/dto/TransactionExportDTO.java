package com.diemlife.dto;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.lowerCase;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.diemlife.utils.CsvUtils.CsvField;
import com.diemlife.utils.CsvUtils.CsvRecord;
import com.diemlife.utils.TransactionResponse.ExportTransactionResponse;


@CsvRecord(header = true, separator = ',')
public class TransactionExportDTO implements Serializable {

    @CsvField(name = "transactions-export.header.order-number", position = 1)
    private String orderNumber;

    @CsvField(name = "transactions-export.header.first-name", position = 2)
    private String firstName;

    @CsvField(name = "transactions-export.header.last-name", position = 3)
    private String lastName;

    @CsvField(name = "transactions-export.header.display-name", position = 4)
    private String displayName;

    @CsvField(name = "transactions-export.header.payment-type", position = 5)
    private String paymentType;

    @CsvField(name = "transactions-export.header.quest-name", position = 6)
    private String questName;

    @CsvField(name = "transactions-export.header.amount", position = 7)
    private String amount;

    @CsvField(name = "transactions-export.header.currency", position = 8)
    private String currency;

    @CsvField(name = "transactions-export.header.amount-refunded", position = 9)
    private String amountRefunded;

    @CsvField(name = "transactions-export.header.discount-code", position = 10)
    private String discountCode;

    @CsvField(name = "transactions-export.header.fee", position = 11)
    private String fee;

    @CsvField(name = "transactions-export.header.net-sale", position = 12)
    private String netSale;

    @CsvField(name = "transactions-export.header.status", position = 13)
    private String status;

    @CsvField(name = "transactions-export.header.created", position = 14)
    private String date;

    @CsvField(name = "transactions-export.header.customer-email", position = 15)
    private String email;

    @CsvField(name = "transactions-export.header.mailing", position = 16)
    private String mailing;

    @CsvField(name = "transactions-export.header.refunded", position = 17)
    private String refunded;

    @CsvField(name = "transactions-export.header.disputed", position = 18)
    private boolean disputed;

    @CsvField(name = "transactions-export.header.dispute.amount", position = 19)
    private String disputeAmount;

    @CsvField(name = "transactions-export.header.dispute.status", position = 20)
    private String disputeStatus;

    @CsvField(name = "transactions-export.header.dispute.reason", position = 21)
    private String disputeReason;

    @CsvField(name = "transactions-export.header.dispute.created", position = 22)
    private String disputeDate;

    @CsvField(name = "transactions-export.header.dispute.evidence", position = 23)
    private String disputeEvidenceDue;

    public TransactionExportDTO() {
        super();
    }

    public static TransactionExportDTO from(final ExportTransactionResponse transaction) {
        final TransactionExportDTO dto = new TransactionExportDTO();
        dto.orderNumber = format("%09d", transaction.id);
        dto.firstName = transaction.from.firstName;
        dto.lastName = transaction.from.lastName;
        dto.displayName = transaction.displayName;
        dto.paymentType = transaction.type == null ? null : capitalize(lowerCase(transaction.type));
        dto.questName = transaction.description;
        dto.currency = transaction.currency;
        dto.amount = transaction.amount == null ? null : format("%.2f", Float.valueOf(transaction.amount) / 100.0f);
        dto.amountRefunded = transaction.refunded == null ? null : format("%.2f", Float.valueOf(transaction.refunded) / 100.0f);
        dto.discountCode = transaction.coupon;
        dto.fee = transaction.fee == null ? null : format("%.2f", Float.valueOf(transaction.fee) / 100.0f);
        dto.netSale = transaction.net == null ? null : format("%.2f", Float.valueOf(transaction.net) / 100.0f);
        dto.status = capitalize(lowerCase(transaction.status));
        dto.date = transaction.created == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(transaction.created));
        dto.email = transaction.from.email;
        dto.mailing = transaction.mailing ? (StringUtils.isBlank(dto.email) ? null : "Yes"): null;
        dto.refunded = transaction.isRefunded;
        dto.disputed = transaction.disputed;
        dto.disputeAmount = transaction.disputeAmount == null ? null : format("%.2f", Float.valueOf(transaction.disputeAmount) / 100.0f);
        dto.disputeStatus = transaction.disputeStatus;
        dto.disputeReason = transaction.disputeReason;
        dto.disputeDate = transaction.disputeCreated == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(transaction.disputeCreated));
        dto.disputeEvidenceDue = transaction.disputeEvidence == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(transaction.disputeEvidence));
        return dto;
    }

}
