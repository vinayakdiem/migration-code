package com.diemlife.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class QuestEdge {

    private long questSrc;
    private String type;
    private long questDst;
    private String tags;
}
