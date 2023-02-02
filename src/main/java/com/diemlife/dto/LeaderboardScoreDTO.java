package com.diemlife.dto;

import lombok.Data;

import javax.annotation.Nullable;
import java.io.Serializable;

@Data
public class LeaderboardScoreDTO implements Serializable {
    private Integer userId;
    private Long memberId;
    private String avatarUrl;
    @Nullable
    private Integer place;
    private String firstName;
    private String lastName;
    private String gender;
    private Integer age;
    private String city;
    private String state;
    private String country;
    private Double score;
    private String unit;
    private String status;
    private String userName;
}
