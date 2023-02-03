package com.diemlife.forms;

import java.util.Date;

public class ParticipantInfoForm extends PersonalInfoForm {

    private String homePhone;
    private String cellPhone;
    private Character gender;
    private Date birthDate;
    private String age;
    private String shirtSize;
    private String burgerTemp;
    private String withCheese;
    private String specialRequests;

    public ParticipantInfoForm() {
    }

    public String getHomePhone() {
        return homePhone;
    }

    public void setHomePhone(final String homePhone) {
        this.homePhone = homePhone;
    }

    public String getCellPhone() {
        return cellPhone;
    }

    public void setCellPhone(final String cellPhone) {
        this.cellPhone = cellPhone;
    }

    public Character getGender() {
        return gender;
    }

    public void setGender(final Character gender) {
        this.gender = gender;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(final Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getAge() {
        return age;
    }

    public void setAge(final String age) {
        this.age = age;
    }

    //These all need to be removed - they should be dynamic
    public String getShirtSize() {
        return shirtSize;
    }

    public void setShirtSize(final String shirtSize) {
        this.shirtSize = shirtSize;
    }

    public String getBurgerTemp() {
        return burgerTemp;
    }

    public void setBurgerTemp(String burgerTemp) {
        this.burgerTemp = burgerTemp;
    }

    public String getWithCheese() {
        return withCheese;
    }

    public void setWithCheese(String withCheese) {
        this.withCheese = withCheese;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

}
