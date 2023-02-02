package com.diemlife.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class UserAvatarDTO implements Serializable {

    @JsonProperty
    public final Integer id;
    @JsonProperty
    public final String firstName;
    @JsonProperty
    public final String lastName;

    @JsonProperty
    public String profilePictureURL = null;

    @JsonCreator
    public UserAvatarDTO(final @NotNull Integer id,
                         final @NotNull String firstName,
                         final @NotNull String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UserAvatarDTO withPhoto(final String profilePictureURL) {
        this.profilePictureURL = profilePictureURL;
        return this;
    }

}
