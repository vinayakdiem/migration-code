package com.diemlife.dto;

public class UserWithFiendStatusDTO extends UserSearchDTO {

    /* Status hints =
        0 = Pending
        1 = Accepted
        2 = Declined
        3 = Blocked
    */
    public Integer friendStatus;

    public UserWithFiendStatusDTO(final Integer id,
                                  final String firstName,
                                  final String lastName,
                                  final String userName,
                                  final String missionStatement,
                                  final String avatarUrl) {
        super(id, firstName, lastName, userName, missionStatement, avatarUrl);
    }

    public UserWithFiendStatusDTO withFriendStatus(final Integer friendStatus) {
        this.friendStatus = friendStatus;
        return this;
    }

}
