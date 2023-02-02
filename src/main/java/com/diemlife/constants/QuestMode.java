package com.diemlife.constants;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.stream.Stream;

public enum QuestMode {

    SUPPORT_ONLY("viewOnlyMode"), PACE_YOURSELF("paceYourselfMode"), TEAM("diyMode");

    private final String key;

    QuestMode(final String key) {
        this.key = key;
    }

    public static QuestMode fromKey(final String key) {
        return Stream.of(values()).filter(value -> value.key.equalsIgnoreCase(key)).findFirst().orElse(null);
    }

    public static class QuestModeKeySerializer extends JsonSerializer<QuestMode> {
        @Override
        public void serialize(final QuestMode value,
                              final JsonGenerator generator,
                              final SerializerProvider serializers) throws IOException {
            generator.writeString(value.key);
        }
    }

    public String getKey() {
        return key;
    }

}
