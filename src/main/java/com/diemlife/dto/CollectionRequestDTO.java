package com.diemlife.dto;

import java.util.List;

import static java.util.Arrays.asList;

public class CollectionRequestDTO extends StripeDTO {
    public String object;
    public Integer limit;
    public String startingAfter;
    public List<String> expand;

    public CollectionRequestDTO(final String... expand) {
        //Stripe does not support empty request fields for expand
        //so we either include it or do not at all
        if (expand != null && expand.length > 0) {
            this.expand = asList(expand);
        }
    }

}
