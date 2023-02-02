/*package com.diemlife.utils;

import com.typesafe.config.Config;
import constants.PaymentMode;
import play.Logger;
import services.StripeConnectService.ExportedCoupon;
import services.StripeConnectService.ExportedProductVariant;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_DOWN;
import static java.math.RoundingMode.HALF_UP;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Singleton
public class FeeUtility {

    private final Double platformTicketFee;
    private final Integer platformTicketSurcharge;
    private final Double stripeCreditCardFee;
    private final Integer stripeCreditCardSurcharge;
    private final Double stripeBankAccountFee;
    private final Integer stripeBankAccountCap;
    private final Double salesTax;

    @Inject
    public FeeUtility(final Config config) {
        platformTicketFee = config.getDouble("application.ticket.fee");
        platformTicketSurcharge = config.getInt("application.ticket.surcharge");
        stripeCreditCardFee = config.getDouble("stripe.creditCard.fee");
        stripeCreditCardSurcharge = config.getInt("stripe.creditCard.surcharge");
        stripeBankAccountFee = config.getDouble("stripe.bankAccount.fee");
        stripeBankAccountCap = config.getInt("stripe.bankAccount.cap");
        salesTax = config.getDouble("application.vat");
    }

    public double calculatePlatformFeePercent(final long initialAmount, long totalAmount, final double platformFee) {
        final BigDecimal netInitialAmount = new BigDecimal(initialAmount);
        final BigDecimal platformFeeValue = new BigDecimal(platformFee).multiply(netInitialAmount).setScale(2, HALF_UP);
        final BigDecimal brutTotalAmount = new BigDecimal(totalAmount);
        return platformFeeValue.multiply(new BigDecimal(100L))
                .divide(brutTotalAmount, HALF_UP)
                .setScale(2, HALF_UP)
                .doubleValue();
    }

    public TransactionBreakdown calculateTicketsFees(final SkuProvider skuProvider,
                                                     final CouponProvider couponProvider,
                                                     final Map<String, Integer> orderItems,
                                                     final String couponCode,
                                                     final PaymentMode paymentMode) {
        if (orderItems == null) {
            return new TransactionBreakdown();
        }

        final List<ExportedProductVariant> variants = orderItems.keySet().stream()
                .map(skuProvider)
                .filter(Objects::nonNull)
                .filter(sku -> sku.active)
                .filter(sku -> sku.price != 0)
                .collect(Collectors.toList());

        final BigDecimal initialNetAmount = BigDecimal.valueOf(variants.stream()
                .mapToLong(sku -> orderItems.getOrDefault(sku.id, 0) * sku.price)
                .sum());

        final ExportedCoupon coupon = isBlank(couponCode) ? null : couponProvider.apply(couponCode);

        if (isFree(initialNetAmount) || hasFreeCoupon(coupon)) {
            final TransactionBreakdown free = new TransactionBreakdown();
            free.netTotal = StrictMath.round(initialNetAmount.doubleValue());
            free.discount = StrictMath.round(initialNetAmount.doubleValue());
            free.tax = 0;
            free.platformFee = 0;
            free.stripeFee = 0;
            free.brutTotal = 0;
            return free;
        }

        final BigDecimal couponPercentageReductionAmount = coupon == null || !coupon.valid
                ? ZERO
                : initialNetAmount.multiply(coupon.percentOff);

        final BigDecimal couponFixedReductionAmount = coupon == null || !coupon.valid
                ? new BigDecimal(0L)
                : new BigDecimal(coupon.amountOff != null ? coupon.amountOff : 0L);

        BigDecimal couponReductionAmount;
        if (couponPercentageReductionAmount.equals(ZERO)) {
            couponReductionAmount = couponFixedReductionAmount;
        } else {
            couponReductionAmount = couponPercentageReductionAmount;
        }

        final BigDecimal totalMinusCouponAmount = initialNetAmount.subtract(couponReductionAmount);

        final BigDecimal platformSurchargeAmount = BigDecimal.valueOf(variants.stream()
                .mapToLong(sku -> (long) orderItems.getOrDefault(sku.id, 0) * platformTicketSurcharge)
                .sum());
        final BigDecimal platformAugmentationAmount = totalMinusCouponAmount
                .multiply(BigDecimal.valueOf(platformTicketFee))
                .add(platformSurchargeAmount);

        final BigDecimal totalWithPlatformAugmentation = totalMinusCouponAmount.add(platformAugmentationAmount);
        final BigDecimal salesTaxAmount = totalWithPlatformAugmentation.multiply(BigDecimal.valueOf(salesTax));
        final BigDecimal totalWithSalesTax = totalWithPlatformAugmentation.add(salesTaxAmount);
        final BigDecimal stripeFees = addFees(paymentMode, totalWithPlatformAugmentation, false);
        final BigDecimal totalWithStripeAugmentation = totalWithSalesTax.add(stripeFees);

        final TransactionBreakdown transactionBreakdown = new TransactionBreakdown();
        transactionBreakdown.netTotal = StrictMath.round(initialNetAmount.doubleValue());
        transactionBreakdown.discount = StrictMath.round(couponReductionAmount.doubleValue());
        transactionBreakdown.platformFee = StrictMath.round(platformAugmentationAmount.doubleValue());
        transactionBreakdown.tax = StrictMath.round(salesTaxAmount.doubleValue());
        transactionBreakdown.stripeFee = StrictMath.round(stripeFees.doubleValue());
        transactionBreakdown.brutTotal = StrictMath.round(totalWithStripeAugmentation.doubleValue());

        Logger.debug("successfully calculated platform fees with transaction breakdown of [{}]", transactionBreakdown);
        return transactionBreakdown;
    }

    public TransactionBreakdown calculateFeesOnTopOfAmount(final BigDecimal totalAmount,
                                                           final BigDecimal tipAmount,
                                                           final PaymentMode paymentMode,
                                                           final BigDecimal platformFee) {

        final BigDecimal platformAugmentationAmount = totalAmount.multiply(platformFee);
        final BigDecimal totalWithPlatformAugmentation = totalAmount.add(platformAugmentationAmount);
        final BigDecimal stripeFees = addFees(paymentMode, totalWithPlatformAugmentation.add(tipAmount), false);
        final BigDecimal totalWithStripeAugmentation = totalWithPlatformAugmentation.add(stripeFees);

        final TransactionBreakdown transactionBreakdown = new TransactionBreakdown();
        transactionBreakdown.netTotal = totalAmount.add(tipAmount).setScale(0, HALF_UP).longValueExact();
        transactionBreakdown.discount = 0;
        transactionBreakdown.platformFee = platformAugmentationAmount.setScale(0, HALF_UP).longValueExact();
        transactionBreakdown.tax = 0;
        transactionBreakdown.stripeFee = stripeFees.setScale(0, HALF_UP).longValueExact();
        transactionBreakdown.brutTotal = totalWithStripeAugmentation.add(tipAmount).setScale(0, HALF_DOWN).longValueExact();
        transactionBreakdown.netTip = tipAmount.setScale(0, HALF_UP).longValueExact();
        // TODO: check if we even need brut tip + fees part for this case, it's supposed to be display only
        transactionBreakdown.brutTip = transactionBreakdown.netTip;

        return transactionBreakdown;
    }

    public TransactionBreakdown calculateFeesIncludedInAmount(final BigDecimal totalAmount,
                                                              final BigDecimal tipAmount,
                                                              final PaymentMode paymentMode,
                                                              final BigDecimal platformFee) {

        final BigDecimal platformAugmentationAmount = totalAmount.multiply(platformFee);
        final BigDecimal stripeFees = addFees(paymentMode, tipAmount.equals(ZERO) ? totalAmount : totalAmount.add(tipAmount), true);

        final TransactionBreakdown transactionBreakdown = new TransactionBreakdown();
        transactionBreakdown.netTotal = totalAmount
                .subtract(platformAugmentationAmount)
                .subtract(addFees(paymentMode, totalAmount , true))
                .setScale(0, HALF_UP)
                .longValueExact();
        transactionBreakdown.discount = 0;
        transactionBreakdown.platformFee = platformAugmentationAmount.setScale(0, HALF_UP).longValueExact();
        transactionBreakdown.tax = 0;
        transactionBreakdown.stripeFee = stripeFees.setScale(0, HALF_UP).longValueExact();
        transactionBreakdown.brutTotal = totalAmount.add(tipAmount).setScale(0, HALF_UP).longValueExact();
        transactionBreakdown.netTip = getNetTip(tipAmount).setScale(0, HALF_DOWN).longValueExact();
        transactionBreakdown.brutTip = tipAmount.setScale(0, HALF_DOWN).longValueExact();

        return transactionBreakdown;
    }

    private BigDecimal addCreditCardFees(final BigDecimal subTotal, final boolean absorbBySeller) {
        final BigDecimal stripeFeeMultiplier = BigDecimal.valueOf(stripeCreditCardFee);
        final BigDecimal stripeSurchargeAmount = BigDecimal.valueOf(stripeCreditCardSurcharge.longValue());
        if (absorbBySeller) {
            return subTotal
                    .multiply(stripeFeeMultiplier)
                    .add(stripeSurchargeAmount);
        } else {
            return subTotal
                    .add(stripeSurchargeAmount)
                    .divide(ONE.subtract(stripeFeeMultiplier), HALF_UP)
                    .subtract(subTotal);
        }
    }

    private BigDecimal getNetTip(final BigDecimal tipAmount) {
        if (ZERO.equals(tipAmount)) {
            return ZERO;
        }
        final BigDecimal stripeFeeMultiplier = BigDecimal.valueOf(stripeCreditCardFee);
        final BigDecimal percent = tipAmount.multiply(stripeFeeMultiplier);
        return tipAmount.subtract(percent);
    }

    private BigDecimal addBankAccountFees(final BigDecimal subTotal, final boolean absorbBySeller) {
        final BigDecimal stripeFeeMultiplier = BigDecimal.valueOf(stripeBankAccountFee);
        final BigDecimal stripeCapAmount = BigDecimal.valueOf(stripeBankAccountCap.longValue());
        final BigDecimal stripeFeeAmount;
        if (absorbBySeller) {
            stripeFeeAmount = subTotal.multiply(stripeFeeMultiplier);
            return stripeFeeAmount.subtract(stripeCapAmount).signum() < 0
                    ? stripeFeeAmount
                    : stripeCapAmount;
        } else {
            stripeFeeAmount = subTotal
                    .divide(ONE.subtract(stripeFeeMultiplier), HALF_UP)
                    .subtract(subTotal);
            return stripeFeeAmount.subtract(stripeCapAmount).signum() < 0
                    ? stripeFeeAmount
                    : stripeCapAmount;
        }
    }

    private BigDecimal addFees(final PaymentMode paymentMode,
                               final BigDecimal subTotal,
                               final boolean absorbBySeller) {
        if (paymentMode == null) {
            return ZERO;
        } else {
            switch (paymentMode) {
                case CreditCard:
                case CreditCardToken:
                    return addCreditCardFees(subTotal, absorbBySeller);
                case BankAccount:
                    return addBankAccountFees(subTotal, absorbBySeller);
                case OfflineMode:
                    return ZERO;
                default:
                    throw new IllegalArgumentException("Unknown payment mode : " + paymentMode);
            }
        }
    }

    private boolean isFree(final BigDecimal initialNetAmount) {
        return ZERO.equals(initialNetAmount);
    }

    private boolean hasFreeCoupon(final ExportedCoupon coupon) {
        return coupon != null && coupon.free;
    }

    public interface SkuProvider extends Function<String, ExportedProductVariant> {
    }

    public interface CouponProvider extends Function<String, ExportedCoupon> {
    }

}
*/