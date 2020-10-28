/*
 * Copyright (c) 2018 WolkAbout Technology s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.wolkabout.wolk.firmwareupdate.model.device2platform;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wolkabout.wolk.firmwareupdate.model.FirmwareUpdateError;
import com.wolkabout.wolk.firmwareupdate.model.FirmwareUpdateStatus;

/**
 * This class represents the payload sent by the device to the platform
 * to the `d2p/firmware_update_status/d/` endpoint to notify the platform of update status.
 */
public class UpdateStatus {

    private FirmwareUpdateStatus status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private FirmwareUpdateError error;

    public UpdateStatus() {
    }

    public UpdateStatus(FirmwareUpdateStatus status) {
        this.status = status;
        this.error = null;
    }

    public UpdateStatus(FirmwareUpdateError error) {
        this.status = FirmwareUpdateStatus.ERROR;
        this.error = error;
    }

    public FirmwareUpdateStatus getStatus() {
        return status;
    }

    public void setStatus(FirmwareUpdateStatus status) {
        this.status = status;
    }

    public FirmwareUpdateError getError() {
        return error;
    }

    public void setError(FirmwareUpdateError error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "UpdateStatus{" +
                "status=" + status +
                ", error=" + error +
                '}';
    }
}
