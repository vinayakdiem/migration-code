package com.diemlife.constants;

/**
 * Not all of these fields will be asked of by Stripe. We want to just ensure that
 * we have them for our records. Along with this, some of them are collected
 * when a user signs up (City, Zip, etc.)
 */
public enum StripeRequiredFields {
    EXTERNAL_ACCOUNT("external_account"),
    CITY("legal_entity.address.city"),
    ADDRESS_LINE_1("legal_entity.address.line1"),
    ADDRESS_POSTAL_CODE("legal_entity.address.postal_code"),
    ADDRESS_STATE("legal_entity.address.state"),
    DOB_DAY("legal_entity.dob.day"),
    DOB_MONTH("legal_entity.dob.month"),
    DOB_YEAR("legal_entity.dob.year"),
    FIRST_NAME("legal_entity.first_name"),
    LAST_NAME("legal_entity.last_name"),
    LAST_4("legal_entity.ssn_last_4"),
    TYPE("legal_entity.type"),
    TOS_ACCEPTANCE_DATE("tos_acceptance.date"),
    TOS_ACCEPTANCE_IP("tos_acceptance.ip"),
    PERSONAL_ID_NUMBER("legal_entity.personal_id_number"),
    VERIFICATION_DOCUMENT("legal_entity.verification.document");

    private final String fieldName;

    StripeRequiredFields(String fieldName) {
        this.fieldName = fieldName;
    }

    public static StripeRequiredFields from(String fieldName) {
        return StripeRequiredFields.valueOf(fieldName);
    }

    public static StripeRequiredFields byFieldName(String fieldName) {
        for (StripeRequiredFields field : StripeRequiredFields.values()) {
            if (field.fieldName().equalsIgnoreCase(fieldName)) {
                return field;
            }
        }
        return null;
    }

    public String fieldName() {
        return this.fieldName;
    }
}
