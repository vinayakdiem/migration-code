package com.diemlife.dto;

import java.io.Serializable;

import com.diemlife.utils.CsvUtils.CsvField;
import com.diemlife.utils.CsvUtils.CsvRecord;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@CsvRecord(header = true, separator = ',')
public class BrandConfigImportDTO implements Serializable {

    @CsvField(name = "brand-config-import.header.action", position = 1)
    private String action;

    @CsvField(name = "brand-config-import.header.brand-name", position = 2)
    private String brandName;

    @CsvField(name = "brand-config-import.header.non-profit", position = 3)
    private String nonProfit;

    @CsvField(name = "brand-config-import.header.site-url", position = 4)
    private String siteUrl;

    @CsvField(name = "brand-config-import.header.logo-url", position = 5)
    private String logoUrl;

    private String ip;
    private String agent;

}
