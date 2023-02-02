package com.diemlife.dto;

import java.io.Serializable;

public class UserSearchDTO implements Serializable {

    public final Integer id;
    public final String firstName;
    public final String lastName;
    public final String userName;
    public final String missionStatement;
    public final String avatarUrl;

    public UserSearchDTO(final Integer id,
                         final String firstName,
                         final String lastName,
                         final String userName,
                         final String missionStatement,
                         final String avatarUrl) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.missionStatement = missionStatement;
        this.avatarUrl = avatarUrl;
    }

    public Integer getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUserName() {
        return userName;
    }

    public String getMissionStatement() {
        return missionStatement;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

}
