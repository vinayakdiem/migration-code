/*package com.diemlife.dto;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.diemlife.models.Happening;
import com.diemlife.models.HappeningAddOn;
import com.diemlife.models.StripeAccount;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import services.HappeningRegisterStatusService;
import services.StripeConnectService;
import services.StripeConnectService.ExportedProduct;

public class HappeningDTO implements Serializable {

    @JsonProperty("happening")
    private final Happening happening;

    @JsonProperty("product")
    private ExportedProduct product;

    @JsonProperty("registerStatus")
    private HappeningRegisterStatusDTO happeningRegisterStatus;

    @JsonProperty("addOns")
    private List<HappeningAddOnDTO> addOns = new ArrayList<>();

    public HappeningDTO(final Happening happening) {
        this.happening = happening;
        if (happening != null) {
            this.happeningRegisterStatus = new HappeningRegisterStatusService(happening).createHappeningRegisterStatus();
            addOns = happening.addOns.stream().map(HappeningAddOnDTO::new).collect(toList());
        }
    }

    public HappeningRegisterStatusDTO getHappeningRegisterStatus() {
        return happeningRegisterStatus;
    }

    public Happening getHappening() {
        return happening;
    }

    public ExportedProduct getProduct() {
        return product;
    }

    public List<HappeningAddOnDTO> getAddOns() {
        return addOns;
    }

    @JsonIgnore
    public HappeningDTO withStripeInfo(final StripeAccount merchant, final StripeConnectService service) {
        if (happening != null && isNotBlank(happening.stripeProductId)) {
            product = service.retrieveProduct(merchant, happening.stripeProductId);
        }
        if (happening != null && isNotEmpty(happening.addOns)) {
            addOns = addOns.stream().map(addOn -> addOn.withStripeInfo(merchant, service)).collect(toList());
        }
        return this;
    }

    public static class HappeningAddOnDTO implements Serializable {

        @JsonProperty("addOn")
        private final HappeningAddOn addOn;

        @JsonProperty("product")
        private ExportedProduct product;

        public HappeningAddOnDTO(final HappeningAddOn addOn) {
            this.addOn = addOn;
        }

        public HappeningAddOn getAddOn() {
            return addOn;
        }

        @JsonIgnore
        public HappeningAddOnDTO withStripeInfo(final StripeAccount merchant, final StripeConnectService service) {
            if (addOn != null && isNotBlank(addOn.stripeProductId)) {
                product = service.retrieveProduct(merchant, addOn.stripeProductId);
            }
            return this;
        }
    }
}
*/