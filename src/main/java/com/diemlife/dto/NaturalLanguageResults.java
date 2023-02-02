package com.diemlife.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class NaturalLanguageResults implements Serializable {

    String category;
    float confidence;
}
