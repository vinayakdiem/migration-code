package com.diemlife.dto;

import java.io.Serializable;

public class SystemFailureDTO implements Serializable {

    public String id;
    public String title;
    public String description;
    public String date;
    public boolean recoverable;
    public String ip;
    public String method;
    public String uri;
    public String message;
    public String stack;

}
