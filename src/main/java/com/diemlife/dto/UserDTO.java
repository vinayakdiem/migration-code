package com.diemlife.dto;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.Objects;

import com.diemlife.constants.Util;
import com.diemlife.models.User;
import com.diemlife.models.UserSEO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDTO extends WhoIAmDTO implements UserSEO {

    public String firstName;
    public String lastName;
    public String name;
    public String avatarUrl;
    public boolean isUserBrand;
    public String userName;
    public Integer userId;
    public String missionStatement;
    public String country;
    public boolean active;
    public String coverPictureURL;
    //ToDO: Add Personal Info
    public Long personalInfo;

    @JsonCreator
    public UserDTO(@JsonProperty("id") final int id, @JsonProperty("email") final String email) {
        super(id, email);
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    public static UserDTO toDTO(final User user) {
        if (user == null) {
            return null;
        }
        final UserDTO dto = new UserDTO(user.getId(), user.getEmail());
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();
        dto.name = user.getName();
        dto.userName = user.getUserName();
        dto.avatarUrl = user.getProfilePictureURL();
        dto.coverPictureURL = user.getCoverPictureURL();
        dto.isUserBrand = "Y".equalsIgnoreCase(user.getIsUserBrand());
        dto.userId = user.getId();
        dto.missionStatement = user.getMissionStatement();
        dto.country = user.getCountry();
        dto.active = user.getActive();
        dto.personalInfo = user.getPersonalInfo()==null?null:user.getPersonalInfo().getId();

        return dto;
    }

    public static List<UserDTO> listToDTO(final List<User> users) {
        if (Util.isEmpty(users)) {
            return emptyList();
        }
        return users.stream().map(UserDTO::toDTO).filter(Objects::nonNull).collect(toList());
    }

}
