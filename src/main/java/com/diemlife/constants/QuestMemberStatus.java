package com.diemlife.constants;

public enum QuestMemberStatus {

    Creator(256),
    Admin(128),
    Backer(64),
    Supporter(32),
    Doer(16),
    Achiever(8),
    Interested(4),
    Unknown(2);

    private int score;

    QuestMemberStatus(final int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }

}
