package com.diemlife.dto;

import java.util.Date;

/**
 * DTO Happening registration status
 * Created 06/11/2020
 *
 * @author SYushchenko
 */
public class HappeningRegisterStatusDTO {
    private Date date;
    private final RegisterState status;

    /**
     * Constructor with one parameters
     *
     * @param status {@link RegisterState}
     */
    public HappeningRegisterStatusDTO(RegisterState status)
    {
        this.status = status;
    }

    /**
     * Constructor with two parameters
     *
     * @param date {@link Date}
     * @param status {@link RegisterState}
     */
    public HappeningRegisterStatusDTO(Date date, RegisterState status) {
        this.date = date;
        this.status = status;
    }

    public Date getDate() {
        return date;
    }

    public RegisterState getStatus() {
        return status;
    }

    /**
     * Register status
     */
    public enum RegisterState {
        REGISTER_START,
        REGISTER_PROGRESS,
        REGISTER_ENDED,
        EVENT_ENDED
    }
}
