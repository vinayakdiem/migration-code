package com.diemlife.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@Builder
@ToString
public class QuestListFilterDTO implements Serializable {

    public boolean active;
    public boolean completed;
    public boolean saved;

}
