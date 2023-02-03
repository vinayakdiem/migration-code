package com.diemlife.utils;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.Barcode;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PushbuttonField;
import com.typesafe.config.Config;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Singleton
public class PDFTemplateStamper {

    private Resource template;

    @Inject
    public PDFTemplateStamper(final Config config) {
        this.template = new ClassPathResource(config.getString("application.ticket.template"));
    }

    public byte[] stampTicket(final Long ticketNumber,
                              final String participantName,
                              final String eventName,
                              final Date eventDate,
                              final URL brandLogo,
                              final String transactionId) {

        try (final ByteArrayOutputStream result = new ByteArrayOutputStream();
             final InputStream input = template.getInputStream()) {

            final PdfReader reader = new PdfReader(input);

            reader.removeUsageRights();

            final PdfStamper stamper = new PdfStamper(reader, result);
            final AcroFields form = stamper.getAcroFields();

            form.setGenerateAppearances(true);

            if (isNotBlank(participantName)) {
                form.setField("participant_name", participantName);
            }
            if (isNotBlank(eventName)) {
                form.setField("event_name", eventName);
            }
            if (eventDate != null) {
                form.setField("event_date", new SimpleDateFormat("MMM dd yyyy").format(eventDate));
            }
            if (ticketNumber != null) {
                form.setField("ticket_number", getFormattedTicketNumber(ticketNumber));
            }

            final PushbuttonField logoField = form.getNewPushbuttonFromField("brand_logo");
            if (logoField != null && brandLogo != null) {
                logoField.setLayout(PushbuttonField.LAYOUT_ICON_ONLY);
                logoField.setProportionalIcon(true);
                logoField.setImage(Image.getInstance(brandLogo));
                form.replacePushbuttonField("brand_logo", logoField.getField());
            }

            final Barcode128 barcode128 = new Barcode128();
            barcode128.setCode(transactionId + " " + getFormattedTicketNumber(ticketNumber));
            barcode128.setCodeType(Barcode.CODE128);
            barcode128.setGenerateChecksum(true);
            barcode128.setChecksumText(false);
            barcode128.setStartStopText(false);
            barcode128.setFont(null);

            final PdfContentByte content = stamper.getOverContent(1);
            final Image code128Image = barcode128.createImageWithBarcode(content, Color.BLACK, Color.BLACK);
            code128Image.setAbsolutePosition(60, 30);
            content.addImage(code128Image);

            stamper.setFormFlattening(true);
            stamper.setFreeTextFlattening(true);

            reader.close();
            stamper.close();

            return result.toByteArray();
        } catch (final IOException | DocumentException e) {
            throw new PDFTemplateStampingException(e);
        }
    }

    private static String getFormattedTicketNumber(final Long ticketNumber) {
        return String.format("%05d", ticketNumber);
    }

    public static class PDFTemplateStampingException extends RuntimeException {
        private PDFTemplateStampingException(final Throwable cause) {
            super(cause);
        }
    }

}
