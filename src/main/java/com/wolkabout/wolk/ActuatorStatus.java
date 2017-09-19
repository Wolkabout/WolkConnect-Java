package com.wolkabout.wolk;

/**
 * Copyright Wolkabout 2017
 */
public class ActuatorStatus {

    private String status;
    private String value;

    public ActuatorStatus(String status, String value) {
        this.status = status;
        this.value = value;
    }

    public ActuatorStatus() {
    }

    public String getStatus() {
        return status;
    }

    public String getValue() {
        return value;
    }
}
