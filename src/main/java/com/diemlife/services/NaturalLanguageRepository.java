package com.diemlife.services;

import com.google.inject.ImplementedBy;
import com.diemlife.dto.NaturalLanguageResults;
import infrastructure.naturallanguage.NaturalLanguageGateway;

import java.util.List;

@ImplementedBy(NaturalLanguageGateway.class)
public interface NaturalLanguageRepository {

    List<NaturalLanguageResults> classify(String description);
}
