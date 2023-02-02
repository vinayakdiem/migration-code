package com.diemlife.models;

import static javax.persistence.FetchType.LAZY;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity(name = "QuestInvites")
@Table(name = "quest_invites", schema = "diemlife")
public class QuestInvite extends IdentifiedEntity {

    @ManyToOne(optional = false, fetch = LAZY)
    @JoinColumn(name = "quest_id", nullable = false, foreignKey = @ForeignKey(name = "quest_invites_quest_id_fk"))
    public Quests quest;

    @ManyToOne(optional = false, fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "quest_invites_user_id_fk"))
    public User user;

    @ManyToOne(optional = false, fetch = LAZY)
    @JoinColumn(name = "invited_user_id", nullable = false, foreignKey = @ForeignKey(name = "quest_invites_invited_user_id_fk"))
    public User invitedUser;

}
