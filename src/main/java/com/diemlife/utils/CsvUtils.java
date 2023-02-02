package com.diemlife.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import play.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.remove;
import static org.apache.commons.lang3.StringUtils.splitPreserveAllTokens;
import static org.springframework.util.ReflectionUtils.doWithFields;
import static org.springframework.util.ReflectionUtils.makeAccessible;
import static org.springframework.util.ReflectionUtils.setField;

public class CsvUtils {

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface CsvRecord {
        boolean header() default false;

        char separator() default ';';
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface CsvField {
        String name() default "";

        int position();
    }

    public static <T> List<T> readCvsFromStream(final Class<T> type,
                                                final InputStream input,
                                                final Charset encoding) {
        final List<T> elements = new ArrayList<>();

        try {
            final List<String> lines = IOUtils.readLines(input, encoding);
            if (CollectionUtils.isEmpty(lines)) {
                return emptyList();
            }

            final AtomicBoolean headerRead = new AtomicBoolean(false);
            final CsvRecord csvConfig = type.getAnnotation(CsvRecord.class);

            lines.forEach(line -> {
                if (csvConfig.header() && !headerRead.get()) {
                    headerRead.set(true);
                } else  {
                    try {
                        final T element = type.newInstance();
                        final String[] tokens = splitPreserveAllTokens(line, csvConfig.separator());

                        doWithFields(type, field -> {
                            final CsvField csvMeta = field.getAnnotation(CsvField.class);
                            final int currentPosition = csvMeta.position();
                            if (currentPosition < 1 || currentPosition > tokens.length) {
                                Logger.warn("Unreachable CSV column " + currentPosition + " of type " + type.getSimpleName());
                            } else {
                                makeAccessible(field);
                                setField(field, element, tokens[currentPosition - 1]);
                            }
                        }, field -> field.isAnnotationPresent(CsvField.class));

                        elements.add(element);

                    } catch (final Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            });
        } catch (final IOException e) {
            Logger.error("Unable to read CSV data of type " + type.getSimpleName(), e);

            return emptyList();
        }

        return elements;
    }

    public static <T> void writeCsvToStream(final List<T> data,
                                            final OutputStream output,
                                            final Charset encoding,
                                            final Function<String, String> headerColumnConverter) {

        final AtomicBoolean headerWritten = new AtomicBoolean(false);
        final ConcurrentMap<Integer, String> headerColumns = new ConcurrentHashMap<>();

        data.stream().filter(element -> element.getClass().isAnnotationPresent(CsvRecord.class)).forEach(element -> {
            final CsvRecord csvConfig = element.getClass().getAnnotation(CsvRecord.class);
            final AtomicInteger maxPosition = new AtomicInteger(0);

            doWithFields(element.getClass(), field -> {
                final CsvField csvMeta = field.getAnnotation(CsvField.class);
                final int currentPosition = csvMeta.position();
                if (csvConfig.header()) {
                    headerColumns.putIfAbsent(currentPosition, headerColumnConverter.apply(csvMeta.name()));
                }
                if (currentPosition > maxPosition.get()) {
                    maxPosition.set(currentPosition);
                }
            }, field -> field.isAnnotationPresent(CsvField.class));
            if (csvConfig.header() && !headerWritten.get()) {
                final String[] headerTokens = new String[maxPosition.get()];
                headerColumns.forEach((key, value) -> headerTokens[key - 1] = value);
                writeTo(output, headerTokens, csvConfig.separator(), encoding);
                headerWritten.set(true);
            }

            final String[] tokens = new String[maxPosition.get()];
            doWithFields(element.getClass(), field -> {
                final int index = field.getAnnotation(CsvField.class).position() - 1;
                if (index >= 0 && index < tokens.length) {
                    makeAccessible(field);
                    final Object value = field.get(element);
                    tokens[index] = value == null ? null : remove(value.toString(), csvConfig.separator());
                }
            }, field -> field.isAnnotationPresent(CsvField.class) && field.getAnnotation(CsvField.class).position() > 0);
            writeTo(output, tokens, csvConfig.separator(), encoding);
        });
        try {
            output.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeTo(final OutputStream output, final String[] tokens, final char separator, final Charset encoding) {
        try {
            output.write(join(tokens, separator).concat("\n").getBytes(encoding));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
