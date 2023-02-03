package com.diemlife.forms;

import play.data.validation.Constraints.Required;

import java.io.Serializable;

public class OrderParticipantForm implements Serializable {

    @Required
    private ParticipantInfoForm participantInfo;
    @Required
    private AddressForm address;
    @Required
    private EmergencyInfoForm emergencyContact;

    private QuestTeamInfoForm teamInfo;

    private String skuId;

    private Integer skuPrice;

    private Integer skuFee;

    public OrderParticipantForm() {
    }

    public ParticipantInfoForm getParticipantInfo() {
        return participantInfo;
    }

    public void setParticipantInfo(final ParticipantInfoForm participantInfo) {
        this.participantInfo = participantInfo;
    }

    public AddressForm getAddress() {
        return address;
    }

    public void setAddress(final AddressForm address) {
        this.address = address;
    }

    public EmergencyInfoForm getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(final EmergencyInfoForm emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public QuestTeamInfoForm getTeamInfo() {
        return teamInfo;
    }

    public void setTeamInfo(final QuestTeamInfoForm teamInfo) {
        this.teamInfo = teamInfo;
    }

    public String getSkuId() {
        return skuId;
    }

    public Integer getSkuPrice() {
        return skuPrice;
    }

    public Integer getSkuFee() {
        return skuFee;
    }

    public void setSkuId(final String skuId) {
        this.skuId = skuId;
    }

    public void setSkuPrice(final Integer skuPrice) {
        this.skuPrice = skuPrice;
    }

    public void setSkuFee(final Integer skuFee) {
        this.skuFee = skuFee;
    }

}
