package com.diemlife.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public abstract class StripeAccountBaseDTO extends StripeDTO {
    public String email;
    public List<String> requestedCapabilities;
    public StripeBusinessType businessType = StripeBusinessType.individual;
    public StripeIndividualDTO individual;
    public TosAcceptanceDTO tosAcceptance;
    public BusinessProfileDTO businessProfile;
    public AccountSettingsDTO settings = new AccountSettingsDTO();

    @Getter
    @Setter
    @NoArgsConstructor
    public static final class TosAcceptanceDTO extends StripeDTO {
        public Long date;
        public String ip;
        public String userAgent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static final class AccountSettingsDTO extends StripeDTO {

        public PayoutsSettingsDTO payouts = new PayoutsSettingsDTO();

        @Getter
        @Setter
        @NoArgsConstructor
        public static final class PayoutsSettingsDTO extends StripeDTO {
            public ScheduleSettingsDTO schedule = new ScheduleSettingsDTO();

            @Getter
            @Setter
            @NoArgsConstructor
            public static final class ScheduleSettingsDTO extends StripeDTO {
                public PayoutsInterval interval = PayoutsInterval.manual;
            }

            public enum PayoutsInterval {
                daily, weekly, monthly, manual
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static final class BusinessProfileDTO extends StripeDTO {
        public String name;
        public String url;
    }

    public enum StripeBusinessType {
        individual, company;
    }
}
