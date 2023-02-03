package com.diemlife.forms;

import play.data.validation.Constraints.Required;

public class OrderBillingForm extends PaymentsEnabledForm {

    @Required
    private PersonalInfoForm personalInfo;
    @Required
    private AddressForm address;

    public OrderBillingForm() {
        super();
    }

    public PersonalInfoForm getPersonalInfo() {
        return personalInfo;
    }

    public void setPersonalInfo(final PersonalInfoForm personalInfo) {
        this.personalInfo = personalInfo;
    }

    public AddressForm getAddress() {
        return address;
    }

    public void setAddress(final AddressForm address) {
        this.address = address;
    }

}
