package com.diemlife.dto;

import java.io.Serializable;

import com.diemlife.utils.CsvUtils.CsvField;
import com.diemlife.utils.CsvUtils.CsvRecord;


@CsvRecord(header = true, separator = ',')
public class ActivityExportDTO implements Serializable {

    @CsvField(name = "activity-export.header.created-date", position = 1)
    private String createdDate;

    @CsvField(name = "activity-export.header.first-name", position = 2)
    private String firstName;

    @CsvField(name = "activity-export.header.last-name", position = 3)
    private String lastName;

    @CsvField(name = "activity-export.header.email", position = 4)
    private String email;

    //@CsvField(name = "activity-export.header.zip-postal-code", position = 5)
    //private String zipCode;

    @CsvField(name = "activity-export.header.activity-name", position = 5)
    private String activityName;

    @CsvField(name = "activity-export.header.activity-attribute-value", position = 6)
    private String attributeValue;

    @CsvField(name = "activity-export.header.activity-attribute-unit", position = 7)
    private String attributeUnit;

    @CsvField(name = "activity-export.header.pillar", position = 8)
    private String pillar;

    @CsvField(name = "activity-export.header.post-title", position = 9)
    private String postTitle;

    @CsvField(name = "activity-export.header.post-description", position = 10)
    private String postDescription;

    @CsvField(name = "activity-export.header.post-image-url", position = 11)
    private String postImageUrl;

        public ActivityExportDTO() {
        super();
    }

    public static ActivityExportDTO from(final LogActivityDTO logActivity) {
        final ActivityExportDTO dto = new ActivityExportDTO();
        dto.createdDate = logActivity.getCreationDateTime(); 
        dto.activityName = logActivity.getActivityName();
        dto.attributeValue = logActivity.getAttributeValue();
        dto.attributeUnit = logActivity.getUnitNamePlural();
        dto.email  = logActivity.getEmail();
        dto.firstName = logActivity.getUserFirstName();
        dto.lastName = logActivity.getUserLastName();
        dto.pillar  = logActivity.getPillarName();
        dto.postDescription = logActivity.getComment().replace("<p>", "").replace("</p>", "");
        dto.postImageUrl = logActivity.getImageURL();
        dto.postTitle  = logActivity.getTitle();
       // dto.zipCode = "123456"
       return dto;
    }

}
