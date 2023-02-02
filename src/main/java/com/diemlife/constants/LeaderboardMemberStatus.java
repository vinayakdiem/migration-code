package com.diemlife.constants;

public enum LeaderboardMemberStatus {
    NoInfo,
    DidNotStart,
    DidNotFinish,
    Started,
    Finished;

    public static LeaderboardMemberStatus fromInt(int status) {
        switch (status) {
            case 1:
                return DidNotStart;
            case 2:
                return DidNotFinish;
            case 3:
                return Started;
            case 4:
                return Finished;
            default:
                return NoInfo;
        }
    }
}
