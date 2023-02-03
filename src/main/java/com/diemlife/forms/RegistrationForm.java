package com.diemlife.forms;

import lombok.Getter;
import lombok.Setter;
import play.data.validation.Constraints.Required;

@Getter
@Setter
public class RegistrationForm {

    @Required(message = "Please enter the users first name")
    private String firstName;
    @Required(message = "Please enter the users last name")
    private String lastName;
    @Required(message = "Please enter the users email address")
    private String email;
    @Required(message = "Please enter the users password")
    private String password1;
    @Required(message = "Please confirm the users password")
    private String password2;
    private String goals;
    private String country;
    private String zip;
    private String receiveEmail;
    private String favPhysical;
    private String favIntel;
    private String favSpirit;
    private String favSocial;
    private String favOcc;
    private String favFin;
    private String favEnv;
    private Boolean withPin;
    //ToDo: New :Check if personal info is taken from the form and enable it
    //private ParticipantInfoForm participantInfo;

    public RegistrationForm() {
        super();
    }

    public RegistrationForm(String firstName, String lastName, String email) {
        super();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    /*public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword1() {
        return password1;
    }

    public void setPassword1(String password1) {
        this.password1 = password1;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    public String getGoals() {
        return goals;
    }

    public void setGoals(String goals) {
        this.goals = goals;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getReceiveEmail() {
        return receiveEmail;
    }

    public void setReceiveEmail(String receiveEmail) {
        this.receiveEmail = receiveEmail;
    }

    public String getFavPhysical() {
        return favPhysical;
    }

    public void setFavPhysical(String favPhysical) {
        this.favPhysical = favPhysical;
    }

    public String getFavIntel() {
        return favIntel;
    }

    public void setFavIntel(String favIntel) {
        this.favIntel = favIntel;
    }

    public String getFavSpirit() {
        return favSpirit;
    }

    public void setFavSpirit(String favSpirit) {
        this.favSpirit = favSpirit;
    }

    public String getFavSocial() {
        return favSocial;
    }

    public void setFavSocial(String favSocial) {
        this.favSocial = favSocial;
    }

    public String getFavOcc() {
        return favOcc;
    }

    public void setFavOcc(String favOcc) {
        this.favOcc = favOcc;
    }

    public String getFavFin() {
        return favFin;
    }

    public void setFavFin(String favFin) {
        this.favFin = favFin;
    }

    public String getFavEnv() {
        return favEnv;
    }

    public void setFavEnv(String favEnv) {
        this.favEnv = favEnv;
    }

    public Boolean getWithPin() {
        return withPin;
    }

    public void setWithPin(Boolean withPin) {
        this.withPin = withPin;
    }

    public Integer getPersonalInfo(){return personalInfo;}

    public void setPersonalInfo(Integer personalInfo) {this.personalInfo = personalInfo;}*/

}
