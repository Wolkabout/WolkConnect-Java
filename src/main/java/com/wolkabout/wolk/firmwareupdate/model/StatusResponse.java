/*
 * Copyright (c) 2018 Wolkabout
 */

package com.wolkabout.wolk.firmwareupdate.model;

import java.util.Objects;

public class StatusResponse {

    private FirmwareStatus status;
    private UpdateError error;

    public FirmwareStatus getStatus() {
        return status;
    }

    public void setStatus(FirmwareStatus status) {
        this.status = status;
    }

    public UpdateError getError() {
        return error;
    }

    public void setError(UpdateError error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "StatusResponse{" +
                "status=" + status +
                ", error=" + error +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatusResponse)) return false;
        StatusResponse that = (StatusResponse) o;
        return error == that.error &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, error);
    }
}
