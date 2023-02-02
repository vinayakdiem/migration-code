package com.diemlife.models;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity(name = "QuestRelatedTransactions")
@Table(name = "payment_transaction")
public abstract class QuestRelatedTransaction extends PaymentTransaction {

    @OneToOne
    @JoinColumn(name = "quest_id", foreignKey = @ForeignKey(name = "fk_transaction_quest_id"))
    public Quests quest;

}
