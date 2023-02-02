package com.diemlife.services;

import com.diemlife.dto.HappeningRegisterStatusDTO;
import lombok.NonNull;
import com.diemlife.models.Happening;

import java.util.Date;

/**
 * Service for determining the status of registration for the event
 * Created 06/11/2020
 *
 * @author SYushchenko
 */
public class HappeningRegisterStatusService {
    private final Happening happening;

    /**
     * Constructor with parameters
     *
     * @param happening {@link Happening}
     */
    public HappeningRegisterStatusService(@NonNull final Happening happening) {
        this.happening = happening;
    }

    /**
     * Create {@link HappeningRegisterStatusDTO} by conditions
     *
     * @return {@link HappeningRegisterStatusDTO}
     */
    public HappeningRegisterStatusDTO createHappeningRegisterStatus() {
        if (validateHappening(happening)) {
            return handlerHappeningRegisterStatus();
        }
        return null;
    }

    private HappeningRegisterStatusDTO handlerHappeningRegisterStatus() {
        Date dateNow = new Date();
        if (registrationNotStarted(dateNow)) {
            return new HappeningRegisterStatusDTO(happening.registerStartDate, HappeningRegisterStatusDTO.RegisterState.REGISTER_START);
        } else if (registrationStarted(dateNow)) {
            return new HappeningRegisterStatusDTO(happening.registerEndDate, HappeningRegisterStatusDTO.RegisterState.REGISTER_PROGRESS);

        } else if (registrationEnded(dateNow)) {
            return new HappeningRegisterStatusDTO(happening.registerEndDate, HappeningRegisterStatusDTO.RegisterState.REGISTER_ENDED);
        }
        return new HappeningRegisterStatusDTO(HappeningRegisterStatusDTO.RegisterState.EVENT_ENDED);
    }

    private boolean registrationStarted(final Date dateNow) {

        return dateNow.after(happening.registerStartDate) && dateNow.before(happening.registerEndDate);
    }

    private boolean registrationNotStarted(final Date dateNow) {
        return dateNow.before(happening.registerStartDate);
    }

    private boolean registrationEnded(Date dateNow) {
        return dateNow.after(happening.registerEndDate) && dateNow.before(happening.happeningDate);
    }

    private boolean validateHappening(final Happening happening) {
        return happening.happeningDate != null
                && happening.registerStartDate != null
                && happening.registerEndDate != null;
    }
}
