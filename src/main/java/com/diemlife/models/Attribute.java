package com.diemlife.models;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.Serializable;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diemlife.constants.ActivityUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

@Getter
public class Attribute implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Attribute.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public long id;
    public String attribute;
    public long questId;
    public long createdBy;
    public Date addedDate;
    public String tags;
    public ActivityUnit unit;

    public Attribute(long id, String attribute, long questId, long createdBy, Date addedDate, String tags, ActivityUnit unit) {
        this.id = id;
        this.attribute = attribute;
        this.questId = questId;
        this.createdBy = createdBy;
        this.addedDate = addedDate;
        this.tags = tags;
        this.unit = unit;
    }

    @JsonProperty("parsedTags")
    public String[] getParsedTags() {
        if (isBlank(tags)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(tags, String[].class);
        } catch (final Exception e) {
            LOGGER.error("Unable to deserialize tags from string: " + tags, e);

            return null;
        }
    }

}
