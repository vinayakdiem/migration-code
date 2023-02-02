package com.diemlife.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class LeaderboardAttributeDTO implements Serializable {
    private String id;
    private String name;
    private String unit;
    private Boolean asc;
}
