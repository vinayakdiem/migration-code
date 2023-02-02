package com.diemlife.utils;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class EmailUtils {

    public static String formatPrice(final Number amount) {
        final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        return currencyFormat.format(amount.floatValue() / 100.0f);
    }

    public static String formatDateTime(final Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        return dateFormat.format(date) + " ET";
    }

}
