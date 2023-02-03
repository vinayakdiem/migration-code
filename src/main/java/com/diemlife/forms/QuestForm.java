package com.diemlife.forms;

import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.Required;

import java.io.Serializable;
import java.util.List;

public abstract class QuestForm implements Serializable {

    @MaxLength(250)
    private String pillar;
    @Required
    @MaxLength(250)
    private String questName;
    @MaxLength(2000)
    private String questShortDescription;
    @MaxLength(30000)
    private String questDescription;
    @MaxLength(15)
    private String privacy;
    @MaxLength(2000)
    private String photo;
    @MaxLength(2000)
    private String questVideoUrl;

    private List<String> invites;
    private List<String> admins;

    private Boolean copyAllowed;
    private Boolean editableMilestones;
    private Boolean leaderboardEnabled;
    private Boolean multiTeamsAllowed;
    private Boolean fundraising;
    private Boolean backBtnDisabled;
    private Boolean milestoneControlsDisabled;
    private Boolean taskViewDisabled;

    private FundraisingConfigForm fundraisingConfig;

    protected QuestForm() {
        super();
    }

    public String getPillar() {
        return pillar;
    }

    public void setPillar(String pillar) {
        this.pillar = pillar;
    }

    public String getQuestName() {
        return questName;
    }

    public void setQuestName(final String questName) {
        this.questName = questName;
    }

    public String getQuestShortDescription() {
        return questShortDescription;
    }

    public void setQuestShortDescription(final String questShortDescription) { this.questShortDescription = questShortDescription; }

    public String getQuestDescription() {
        return questDescription;
    }

    public void setQuestDescription(final String questDescription) {
        this.questDescription = questDescription;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(final String privacy) {
        this.privacy = privacy;
    }

    public List<String> getInvites() {
        return invites;
    }

    public void setInvites(final List<String> invites) {
        this.invites = invites;
    }

    public List<String> getAdmins() {
        return admins;
    }

    public void setAdmins(final List<String> admins) {
        this.admins = admins;
    }

    public Boolean getCopyAllowed() {
        return copyAllowed;
    }

    public void setCopyAllowed(final Boolean copyAllowed) {
        this.copyAllowed = copyAllowed;
    }

    public Boolean getEditableMilestones() {
        return editableMilestones;
    }

    public void setEditableMilestones(final Boolean editableMilestones) { this.editableMilestones = editableMilestones; }

    public Boolean getLeaderboardEnabled() {
        return leaderboardEnabled;
    }

    public void setLeaderboardEnabled(final Boolean leaderboardEnabled) {
        this.leaderboardEnabled = leaderboardEnabled;
    }

    public Boolean getMultiTeamsAllowed() {
        return multiTeamsAllowed;
    }

    public void setMultiTeamsAllowed(final Boolean multiTeamsAllowed) {
        this.multiTeamsAllowed = multiTeamsAllowed;
    }

    public Boolean getFundraising() {
        return fundraising;
    }

    public void setFundraising(final Boolean fundraising) {
        this.fundraising = fundraising;
    }

    public FundraisingConfigForm getFundraisingConfig() {
        return fundraisingConfig;
    }

    public void setFundraisingConfig(final FundraisingConfigForm fundraisingConfig) { this.fundraisingConfig = fundraisingConfig; }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getQuestVideoUrl() {
        return questVideoUrl;
    }

    public void setQuestVideoUrl(final String questVideoUrl) {
        this.questVideoUrl = questVideoUrl;
    }

    public Boolean getBackBtnDisabled() {
        return backBtnDisabled;
    }

    public void setBackBtnDisabled(Boolean backBtnDisabled) {
        this.backBtnDisabled = backBtnDisabled;
    }

    public Boolean getMilestoneControlsDisabled() { return milestoneControlsDisabled; }

    public void setMilestoneControlsDisabled(Boolean milestoneControlsDisabled) { this.milestoneControlsDisabled = milestoneControlsDisabled; }

    public Boolean getTaskViewDisabled() { return taskViewDisabled; }

    public void setTaskViewDisabled(Boolean taskViewDisabled) { this.taskViewDisabled = taskViewDisabled; }

}
